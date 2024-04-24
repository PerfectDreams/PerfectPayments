package net.perfectdreams.perfectpayments.backend.utils

import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus

object MercadoPagoUtils {
    fun getPaymentStatusFromMercadoPagoPaymentStatus(status: String): PaymentStatus? {
        return when (status) {
            "approved" -> PaymentStatus.APPROVED
            "in_mediation" -> PaymentStatus.CHARGED_BACK
            "charged_back" -> PaymentStatus.CHARGED_BACK
            else -> null
        }
    }
}