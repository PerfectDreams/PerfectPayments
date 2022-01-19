package net.perfectdreams.perfectpayments.common.data.api

import kotlinx.serialization.Serializable

@Serializable
data class CreatePaymentRequest(
    val title: String,
    val currencyId: String,
    val amount: Long,
    val callbackUrl: String,
    val externalReference: String
)