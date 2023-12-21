package net.perfectdreams.perfectpayments.backend.utils

import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus

object PagSeguroUtils {
    fun getPaymentStatusFromPagSeguroPaymentStatus(status: Int): PaymentStatus? {
        // I don't remember from where I got these status IDs
        return when (status) {
            3, 4 -> PaymentStatus.APPROVED
            5, 9 -> PaymentStatus.CHARGED_BACK
            else -> null
        }
    }
}