package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable

/**
 * Same as [PartialPayment], but only has the data needed for the frontend
 */
@Serializable
data class ClientSidePartialPayment(
    val title: String,
    val amount: Long,
    val currencyId: String
)