package net.perfectdreams.perfectpayments

import club.minnced.discord.webhook.WebhookClient
import com.github.benmanes.caffeine.cache.Caffeine
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.client.HttpClient
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.http.content.staticRootFolder
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.config.AppConfig
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.processors.creators.PagSeguroPaymentCreator
import net.perfectdreams.perfectpayments.processors.creators.PayPalPaymentCreator
import net.perfectdreams.perfectpayments.processors.creators.PicPayPaymentCreator
import net.perfectdreams.perfectpayments.processors.creators.StripePaymentCreator
import net.perfectdreams.perfectpayments.routes.CancelledRoute
import net.perfectdreams.perfectpayments.routes.HomeRoute
import net.perfectdreams.perfectpayments.routes.SuccessRoute
import net.perfectdreams.perfectpayments.routes.api.v1.callbacks.PostPagSeguroCallbackRoute
import net.perfectdreams.perfectpayments.routes.api.v1.callbacks.PostPayPalCallbackRoute
import net.perfectdreams.perfectpayments.routes.api.v1.callbacks.PostPicPayCallbackRoute
import net.perfectdreams.perfectpayments.routes.api.v1.callbacks.PostStripeCallbackRoute
import net.perfectdreams.perfectpayments.routes.api.v1.payments.PatchChangePaymentStatusRoute
import net.perfectdreams.perfectpayments.routes.api.v1.payments.PostCreatePaymentRoute
import net.perfectdreams.perfectpayments.routes.api.v1.payments.PostStartPaymentRoute
import net.perfectdreams.perfectpayments.routes.checkout.CheckoutRoute
import net.perfectdreams.perfectpayments.routes.checkout.PostCheckoutPicPayRoute
import net.perfectdreams.perfectpayments.routes.checkout.PostCheckoutRoute
import net.perfectdreams.perfectpayments.tables.Payments
import net.perfectdreams.perfectpayments.utils.GatewayConfigs
import net.perfectdreams.perfectpayments.utils.PartialPayment
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.util.*

class PerfectPayments(
    val config: AppConfig,
    gatewayConfigs: Map<PaymentGateway, Any>
) {
    companion object {
        val http = HttpClient {
            expectSuccess = false
        }
        const val USER_AGENT = "PerfectPayments"
        const val DEFAULT_LOCALE_ID = "default"
        val YAML = Yaml()
        private val logger = KotlinLogging.logger {}
    }

    var locales = mapOf<String, BaseLocale>()
    val gateway = GatewayConfigs(gatewayConfigs)
    val discordWebhook = config.discordNotificationsWebhook?.let {
        WebhookClient.withUrl(it)
    }

    /**
     * Partial payment cache, this will is used when the payment does not have any gateway payment bound
     * (user haven't selected the payment gateway well)
     */
    val partialPayments = Caffeine.newBuilder()
        .maximumSize(100_000)
        .build<UUID, PartialPayment>()
        .asMap()

    val paymentCreators = mapOf(
        PaymentGateway.PICPAY to PicPayPaymentCreator(this),
        PaymentGateway.PAGSEGURO to PagSeguroPaymentCreator(this),
        PaymentGateway.STRIPE to StripePaymentCreator(this),
        PaymentGateway.PAYPAL to PayPalPaymentCreator(this)
    )

    val routes = listOf(
        HomeRoute(),
        CheckoutRoute(this),
        PostCheckoutRoute(this),
        PostCheckoutPicPayRoute(this),
        SuccessRoute(this),
        CancelledRoute(this),

        // ===[ API ]===
        PostCreatePaymentRoute(this),
        PostStartPaymentRoute(this),
        PatchChangePaymentStatusRoute(this),

        // Callbacks
        PostPicPayCallbackRoute(this),
        PostPagSeguroCallbackRoute(this),
        PostStripeCallbackRoute(this),
        PostPayPalCallbackRoute(this)
    )

    fun start() {
        loadLocales()

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = "jdbc:postgresql://${config.database.address}/${config.database.databaseName}"
        hikariConfig.username = config.database.username
        if (config.database.password != null)
            hikariConfig.password = config.database.password

        val ds = HikariDataSource(hikariConfig)
        Database.connect(ds)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(Payments)
        }

        val server = embeddedServer(Netty, host = config.website.host, port = config.website.port) {
            routing {
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

    /**
     * Gets the BaseLocale from the ID, if the locale doesn't exist, the default locale ("default") will be retrieved
     *
     * @param localeId the ID of the locale
     * @return         the locale on BaseLocale format or, if the locale doesn't exist, the default locale will be loaded
     * @see            LegacyBaseLocale
     */
    fun getLocaleById(localeId: String): BaseLocale {
        return locales.getOrDefault(localeId, locales[DEFAULT_LOCALE_ID]!!)
    }

    /**
     * Initializes the [id] locale and adds missing translation strings to non-default languages
     *
     * @see BaseLocale
     */
    fun loadLocale(id: String, defaultLocale: BaseLocale?): BaseLocale {
        val locale = BaseLocale(id)
        if (defaultLocale != null) {
            // Colocar todos os valores padrões
            locale.localeStringEntries.putAll(defaultLocale.localeStringEntries)
            locale.localeListEntries.putAll(defaultLocale.localeListEntries)
        }

        val localeFolder = File(config.localeFolder, id)

        // Does exactly what the variable says: Only matches single quotes (') that do not have a slash (\) preceding it
        // Example: It's me, Mario!
        // But if there is a slash preceding it...
        // Example: \'{@user}\'
        // It won't match!
        val singleQuotesWithoutSlashPrecedingItRegex = Regex("(?<!(?:\\\\))'")

        if (localeFolder.exists()) {
            localeFolder.listFiles().filter { it.extension == "yml" || it.extension == "json" }.forEach {
                val entries = YAML.load<MutableMap<String, Any?>>(it.readText())

                fun transformIntoFlatMap(map: MutableMap<String, Any?>, prefix: String) {
                    map.forEach { (key, value) ->
                        if (value is Map<*, *>) {
                            transformIntoFlatMap(value as MutableMap<String, Any?>, "$prefix$key.")
                        } else {
                            if (value is List<*>) {
                                locale.localeListEntries[prefix + key] = try {
                                    (value as List<String>).map {
                                        it.replace(singleQuotesWithoutSlashPrecedingItRegex, "''") // Escape single quotes
                                            .replace("\\'", "'") // Replace \' with '
                                    }
                                } catch (e: ClassCastException) {
                                    // A LinkedHashMap does match the "is List<*>" check, but it fails when we cast the subtype to String
                                    // If that happens, we will just ignore the exception and use the raw "value" list.
                                    (value as List<String>)
                                }
                            } else if (value is String) {
                                locale.localeStringEntries[prefix + key] = value.replace(singleQuotesWithoutSlashPrecedingItRegex, "''") // Escape single quotes
                                    .replace("\\'", "'") // Replace \' with '
                            } else throw IllegalArgumentException("Invalid object type detected in YAML! $value")
                        }
                    }
                }

                transformIntoFlatMap(entries, "")
            }
        }

        // Before we say "okay everything is OK! Let's go!!" we are going to format every single string on the locale
        // to check if everything is really OK
        for ((key, string) in locale.localeStringEntries) {
            try {
                string?.format()
            } catch (e: IllegalArgumentException) {
                logger.error("String \"$string\" stored in \"$key\" from $id can't be formatted! If you are using {...} formatted placeholders, do not forget to add \\' before and after the placeholder!")
                throw e
            }
        }

        return locale
    }

    /**
     * Initializes the available locales and adds missing translation strings to non-default languages
     *
     * @see BaseLocale
     */
    fun loadLocales() {
        val locales = mutableMapOf<String, BaseLocale>()

        val localeFolder = File(config.localeFolder)

        val defaultLocale = loadLocale(DEFAULT_LOCALE_ID, null)
        locales[DEFAULT_LOCALE_ID] = defaultLocale

        localeFolder.listFiles().filter { it.isDirectory && it.name != DEFAULT_LOCALE_ID && !it.name.startsWith(".") /* ignorar .git */ && it.name != "legacy" /* Do not try to load legacy locales */ }.forEach {
            locales[it.name] = loadLocale(it.name, defaultLocale)
        }

        for ((localeId, locale) in locales) {
            val languageInheritsFromLanguageId = locale["loritta.inheritsFromLanguageId"]

            if (languageInheritsFromLanguageId != DEFAULT_LOCALE_ID) {
                // Caso a linguagem seja filha de outra linguagem que não seja a default, nós iremos recarregar a linguagem usando o pai correto
                // Isso é útil já que linguagens internacionais seriam melhor que dependa de "en-us" em vez de "default".
                // Também seria possível implementar "linguagens auto geradas" com overrides específicos, por exemplo: "auto-en-us" -> "en-us"
                locales[localeId] = loadLocale(localeId, locales[languageInheritsFromLanguageId])
            }
        }

        this.locales = locales
    }
}