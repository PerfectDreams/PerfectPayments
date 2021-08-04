package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway

@Serializable
data class FilledPartialPayment(
    val gateway: PaymentGateway,
    val personalData: PersonalData? = null,
    val picPayPersonalData: PicPayPersonalData? = null
)