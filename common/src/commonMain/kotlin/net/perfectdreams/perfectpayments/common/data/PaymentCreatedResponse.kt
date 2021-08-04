package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable

@Serializable
data class PaymentCreatedResponse(
    val url: String
)