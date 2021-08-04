package net.perfectdreams.perfectpayments.utils

data class PartialPayment(
    val title: String,
    val amount: Long,
    val currencyId: String,
    val callbackUrl: String,
    val externalReference: String
)