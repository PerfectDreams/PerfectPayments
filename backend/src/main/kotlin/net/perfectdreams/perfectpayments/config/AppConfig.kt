package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway

@Serializable
data class AppConfig(
        val localeFolder: String,
        val notificationToken: String,
        val database: DatabaseConfig,
        val website: WebsiteConfig,
        val gateways: List<PaymentGateway>,
        val tokens: List<TokenConfig>,
        val discordNotificationsWebhook: String? = null
)