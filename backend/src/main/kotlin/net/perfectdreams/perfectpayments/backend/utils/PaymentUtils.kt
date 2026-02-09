package net.perfectdreams.perfectpayments.backend.utils

import kotlinx.datetime.Clock
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import java.math.BigDecimal

object PaymentUtils {
    private val logger = KotlinLogging.logger {}

    /**
     * Updates a [payment] status and relays the new [status] to the payment's callback
     *
     * This also handles issuing/cancellation of notas fiscais, if needed
     *
     * @param m       the PerfectPayments instance
     * @param payment the payment
     * @param status  the new status
     * @param nfsePaymentValue overrides the NFS-e payment value, used for MercadoPago stuff
     */
    suspend fun updatePaymentStatus(
        m: PerfectPayments,
        payment: Payment,
        status: PaymentStatus,
        nfsePaymentValue: BigDecimal? = null,
        netReceivedAmount: Long? = null
    ) {
        if (payment.status == status) { // Same status as before, no need to update it
            logger.warn { "Payment ${payment.id} already has the status $status, so we aren't going to update it..." }
            return
        }

        logger.info { "Setting payment ${payment.id} to $status!" }

        m.newSuspendedTransaction {
            // Update the status
            if (status == PaymentStatus.APPROVED) {
                payment.paidAt = Clock.System.now()
                if (netReceivedAmount != null)
                    payment.netReceivedAmount = netReceivedAmount
            }
            payment.status = status
        }

        // Send a update to the callback URL
        PaymentQuery.sendPaymentNotification(m, payment)

        if (payment.status == PaymentStatus.APPROVED) {
            // Issue notas fiscais if the payment was approved
            m.notaFiscais?.generateNotaFiscal(payment, nfsePaymentValue)
        } else if (payment.status == PaymentStatus.CHARGED_BACK) {
            // Cancel notas fiscais if the payment was charged back
            m.notaFiscais?.cancelNotaFiscais(payment, "Usuário solicitou reembolso do pagamento")
        }
    }
}