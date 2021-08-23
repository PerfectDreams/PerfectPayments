package net.perfectdreams.perfectpayments.backend.config

import kotlinx.serialization.Serializable

@Serializable
class PayPalConfig(
    val clientId: String,
    val clientSecret: String,
    val isSandbox: Boolean,
    val webhookId: String
) {
    fun getBaseUrl(): String {
        return if (isSandbox) {
            "https://api.sandbox.paypal.com"
        } else {
            "https://api.paypal.com"
        }
    }
}