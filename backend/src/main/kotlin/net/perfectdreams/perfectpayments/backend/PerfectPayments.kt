package net.perfectdreams.perfectpayments.backend

import club.minnced.discord.webhook.WebhookClient
import com.github.benmanes.caffeine.cache.Caffeine
import com.mercadopago.MercadoPagoConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.config.AppConfig
import net.perfectdreams.perfectpayments.backend.config.FocusNFeConfig
import net.perfectdreams.perfectpayments.backend.processors.creators.*
import net.perfectdreams.perfectpayments.backend.routes.CancelledRoute
import net.perfectdreams.perfectpayments.backend.routes.HomeRoute
import net.perfectdreams.perfectpayments.backend.routes.MissingPartialPaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.SuccessRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.GetAvailableGatewaysRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.GetStringsRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.*
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.GetPartialPaymentInfoRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.GetReissueNotaFiscalForPaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.GetRenotifyPaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.PatchChangePaymentStatusRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.PostCreatePaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.api.v1.payments.PostFinishPartialPaymentRoute
import net.perfectdreams.perfectpayments.backend.routes.checkout.CheckoutRoute
import net.perfectdreams.perfectpayments.backend.tables.FocusNFeEvents
import net.perfectdreams.perfectpayments.backend.tables.NotaFiscais
import net.perfectdreams.perfectpayments.backend.tables.PaymentPersonalInfos
import net.perfectdreams.perfectpayments.backend.tables.Payments
import net.perfectdreams.perfectpayments.backend.utils.*
import net.perfectdreams.perfectpayments.backend.utils.focusnfe.FocusNFe
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import java.io.File
import java.sql.Connection
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes

class PerfectPayments(
    val config: AppConfig,
    val focusNFeConfig: FocusNFeConfig? = null,
    gatewayConfigs: Map<PaymentGateway, Any>
) {
    companion object {
        val http = HttpClient(Apache) {
            expectSuccess = false
            install(HttpTimeout) {
                // Because FocusNFe is kinda finicky ngl
                // "Request timeout has expired [url=https://api.focusnfe.com.br/v2/nfse?ref=pp-prod-1507, request_timeout=1000 ms]"
                requestTimeoutMillis = 15_000
            }
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
    val tasksScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

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
        PaymentGateway.SANDBOX to SandboxPaymentCreator(this),
        PaymentGateway.MERCADOPAGO to MercadoPagoPaymentCreator(this)
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
        PostFinishPartialPaymentRoute(this),
        GetReissueNotaFiscalForPaymentRoute(this),
        GetRenotifyPaymentRoute(this)
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

        if (config.gateways.contains(PaymentGateway.MERCADOPAGO)) {
            it.add(PostMercadoPagoCallbackRoute(this))
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
                NotaFiscais,
                FocusNFeEvents
            )
        }

        if (config.gateways.contains(PaymentGateway.PAGSEGURO)) {
            scheduleCoroutineAtFixedRate(UpdatePagSeguroPaymentsTask::class.simpleName!!, tasksScope, 1.minutes, action = UpdatePagSeguroPaymentsTask(this))
        }

        if (config.gateways.contains(PaymentGateway.MERCADOPAGO)) {
            MercadoPagoConfig.setAccessToken(gateway.mercadoPago.accessToken) // Nasty!!
            scheduleCoroutineAtFixedRate(UpdateMercadoPagoPaymentsTask::class.simpleName!!, tasksScope, 1.minutes, action = UpdateMercadoPagoPaymentsTask(this))
        }

        val server = embeddedServer(Netty, host = config.website.host, port = config.website.port) {
            install(CORS) {
                anyHost()
            }

            // Enables gzip and deflate compression
            install(Compression)

            // Enables caching for the specified types in the typesToCache list
            install(CachingHeaders) {
                options { call, outgoingContent ->
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

        Runtime.getRuntime().addShutdownHook(
            Thread {
                logger.info { "Shutting down PerfectPayments..." }

                // Wait until the requests are processed before shutting down the server
                server.stop(5, 25, TimeUnit.SECONDS)
            }
        )
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