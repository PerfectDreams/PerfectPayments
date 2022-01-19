package net.perfectdreams.perfectpayments.common.data.api

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentResponse(
    val id: String, // TODO: This is actually a UUID, should be handled as a UUID (However there isn't mpp UUID yet)
    val paymentUrl: String
)