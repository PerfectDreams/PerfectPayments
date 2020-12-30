package net.perfectdreams.perfectpayments.config

import kotlinx.serialization.Serializable
import net.perfectdreams.perfectpayments.payments.PaymentGateway

@Serializable
data class TokenConfig(
        val name: String,
        val description: String
)