package net.perfectdreams.perfectpayments

import com.stripe.Stripe
import com.typesafe.config.ConfigFactory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.config.*
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import java.io.File

@ExperimentalSerializationApi
object PerfectPaymentsLauncher {
    private val logger = KotlinLogging.logger {}
    
    @JvmStatic
    fun main(args: Array<String>) {
        val config = loadConfig<AppConfig>("./app.conf")

        logger.info { "Payment Gateways: ${config.gateways}" }

        val configs = mutableMapOf<PaymentGateway, Any>()

        for (gateway in config.gateways) {
            if (gateway == PaymentGateway.PICPAY) {
                configs[gateway] = loadConfig<PicPayConfig>("./picpay.conf")
            }
            if (gateway == PaymentGateway.PAGSEGURO) {
                configs[gateway] = loadConfig<PagSeguroConfig>("./pagseguro.conf")
            }
            if (gateway == PaymentGateway.STRIPE) {
                configs[gateway] = loadConfig<StripeConfig>("./stripe.conf")
                    .apply { Stripe.apiKey = secretToken }
            }
            if (gateway == PaymentGateway.PAYPAL) {
                configs[gateway] = loadConfig<PayPalConfig>("./paypal.conf")
            }
        }

        val pp = PerfectPayments(config, configs)
        pp.start()
    }

    inline fun <reified T> loadConfig(path: String): T {
        val lightbendConfig = ConfigFactory.parseFile(File(path))
                .resolve()

        return Hocon.decodeFromConfig(lightbendConfig)
    }
}