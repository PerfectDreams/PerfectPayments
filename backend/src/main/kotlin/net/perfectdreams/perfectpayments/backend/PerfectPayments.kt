package net.perfectdreams.perfectpayments.backend

import club.minnced.discord.webhook.WebhookClient
import com.github.benmanes.caffeine.cache.Caffeine
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.config.AppConfig
import net.perfectdreams.perfectpayments.backend.config.FocusNFeConfig
import net.perfectdreams.perfectpayments.backend.processors.creators.PagSeguroPaymentCreator
import net.perfectdreams.perfectpayments.backend.processors.creators.PayPalPaymentCreator
import net.perfectdreams.perfectpayments.backend.processors.creators.PicPayPaymentCreator
import net.perfectdreams.perfectpayments.backend.processors.creators.SandboxPaymentCreator
import net.perfectdreams.perfectpayments.backend.processors.creators.StripePaymentCreator
import net.perfectdreams.perfectpayments.backend.routes.CancelledRoute
import net.perfectdreams.perfectpayments.backend.routes.HomeRoute
import net.perfectdreams.perfectpayments.backend.routes.MissingPartialPaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.SuccessRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.GetAvailableGatewaysRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.GetStringsRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostFocusNFeCallbackRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostPagSeguroCallbackRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostPayPalCallbackRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostPicPayCallbackRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostStripeCallbackRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.GetPartialPaymentInfoRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.PatchChangePaymentStatusRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.PostCreatePaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.PostFinishPartialPaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.checkout.CheckoutRoute
import net.perfectdreams.perfectpayments.backend.tables.NotaFiscais
import net.perfectdreams.perfectpayments.backend.tables.PaymentPersonalInfos
import net.perfectdreams.perfectpayments.backend.tables.Payments
import net.perfectdreams.perfectpayments.backend.utils.GatewayConfigs
import net.perfectdreams.perfectpayments.backend.utils.LanguageManager
import net.perfectdreams.perfectpayments.backend.utils.NotaFiscalUtils
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment
import net.perfectdreams.perfectpayments.backend.utils.WebsiteAssetsHashManager
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.FocusNFe
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.sql.Connection
import java.util.*

class PerfectPayments(
    val config: AppConfig,
    val focusNFeConfig: FocusNFeConfig? = null,
    gatewayConfigs: Map<PaymentGateway, Any>
) {
    companion object {
        val http = HttpClient {
            expectSuccess = false
        }
        const val USER_AGENT = "PerfectPayments"
        private val logger = KotlinLogging.logger {}
    }

    val languageManager = LanguageManager("en", "/languages/")
    val hashManager = WebsiteAssetsHashManager()
    val gateway = GatewayConfigs(gatewayConfigs)
    val discordWebhook = config.discordNotificationsWebhook?.let {
        WebhookClient.withUrl(it)
    }

    /**
     * Partial payment cache, this will is used when the payment does not have any gateway payment bound
     * (user haven't selected the payment gateway yet)
     */
    val partialPayments = Caffeine.newBuilder()
        .maximumSize(100_000)
        .build<UUID, PartialPayment>()
        .asMap()

    val paymentCreators = mapOf(
        PaymentGateway.PICPAY to PicPayPaymentCreator(this),
        PaymentGateway.PAGSEGURO to PagSeguroPaymentCreator(this),
        PaymentGateway.STRIPE to StripePaymentCreator(this),
        PaymentGateway.PAYPAL to PayPalPaymentCreator(this),
        PaymentGateway.SANDBOX to SandboxPaymentCreator(this)
    )

    val focusNFe = focusNFeConfig?.let {
        FocusNFe(it)
    }

    val notaFiscais = focusNFeConfig?.let {
        NotaFiscalUtils(this, focusNFe!!, it.referencePrefix)
    }

    val routes = mutableListOf(
        HomeRoute(),
        CheckoutRoute(this),
        SuccessRoute(this),
        CancelledRoute(this),
        MissingPartialPaymentRoute(this),

        // ===[ API ]===
        GetStringsRoute(this),
        GetAvailableGatewaysRoute(this),
        GetPartialPaymentInfoRoute(this),
        PostCreatePaymentRoute(this),
        PatchChangePaymentStatusRoute(this),
        PostFinishPartialPaymentRoute(this)
    ).also {
        // Only register routes if gateway is enabled
        if (focusNFeConfig != null)
            it.add(PostFocusNFeCallbackRoute(this, focusNFeConfig))

        if (config.gateways.contains(PaymentGateway.PICPAY)) {
            it.add(PostPicPayCallbackRoute(this))
        }

        if (config.gateways.contains(PaymentGateway.PAGSEGURO)) {
            it.add(PostPagSeguroCallbackRoute(this))
        }

        if (config.gateways.contains(PaymentGateway.STRIPE)) {
            it.add(PostStripeCallbackRoute(this))
        }

        if (config.gateways.contains(PaymentGateway.PAYPAL)) {
            it.add(PostPayPalCallbackRoute(this))
        }

        /* if (config.gateways.contains(PaymentGateway.SANDBOX))
            it.add(PostCheckoutSandboxRoute(this)) */
    }

    private val typesToCache = listOf(
        ContentType.Text.CSS,
        ContentType.Text.JavaScript,
        ContentType.Application.JavaScript,
        ContentType.Image.Any,
        ContentType.Video.Any
    )

    fun start() {
        languageManager.loadLanguagesAndContexts()

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:postgresql://${config.database.address}/${config.database.databaseName}"
        hikariConfig.username = config.database.username
        if (config.database.password != null)
            hikariConfig.password = config.database.password

        // https://github.com/JetBrains/Exposed/wiki/DSL#batch-insert
        hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true")

        // Exposed uses autoCommit = false, so we need to set this to false to avoid HikariCP resetting the connection to
        // autoCommit = true when the transaction goes back to the pool, because resetting this has a "big performance impact"
        // https://stackoverflow.com/a/41206003/7271796
        hikariConfig.isAutoCommit = false

        // Useful to check if a connection is not returning to the pool, will be shown in the log as "Apparent connection leak detected"
        hikariConfig.leakDetectionThreshold = 30 * 1000

        // We need to use the same transaction isolation used in Exposed, in this case, TRANSACTION_READ_COMMITED.
        // If not HikariCP will keep resetting to the default when returning to the pool, causing performance issues.
        hikariConfig.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        val ds = HikariDataSource(hikariConfig)
        Database.connect(ds)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Payments,
                PaymentPersonalInfos,
                NotaFiscais
            )
        }

        val server = embeddedServer(Netty, host = config.website.host, port = config.website.port) {
            install(CORS) {
                anyHost()
            }

            // Enables gzip and deflate compression
            install(Compression)

            // Enables caching for the specified types in the typesToCache list
            install(CachingHeaders) {
                options { outgoingContent ->
                    val contentType = outgoingContent.contentType
                    if (contentType != null) {
                        val contentTypeWithoutParameters = contentType.withoutParameters()
                        val matches = typesToCache.any { contentTypeWithoutParameters.match(it) || contentTypeWithoutParameters == it }

                        if (matches)
                            CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 365 * 24 * 3600))
                        else
                            null
                    } else null
                }
            }

            routing {
                static("/assets/") {
                    resources("static/assets/")
                }

                static("assets") {
                    staticRootFolder = File(config.website.dataFolder, "static")
                    files("assets")
                }

                for (route in routes) {
                    route.register(this)
                }
            }
        }
        server.start(wait = true)
    }

    fun <T> transaction(repetitions: Int = 5, transactionIsolation: Int = Connection.TRANSACTION_REPEATABLE_READ, statement: Transaction.() -> T) = org.jetbrains.exposed.sql.transactions.transaction(transactionIsolation, repetitions) {
        statement.invoke(this)
    }

    suspend fun <T> newSuspendedTransaction(repetitions: Int = 5, transactionIsolation: Int = Connection.TRANSACTION_REPEATABLE_READ, statement: Transaction.() -> T): T = withContext(Dispatchers.IO) {
        transaction(repetitions, transactionIsolation) {
            statement.invoke(this)
        }
    }
}