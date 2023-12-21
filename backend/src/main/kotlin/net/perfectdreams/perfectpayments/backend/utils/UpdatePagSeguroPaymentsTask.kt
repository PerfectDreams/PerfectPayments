package net.perfectdreams.perfectpayments.backend.utils

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.routes.api.v1.callbacks.PostPagSeguroCallbackRoute
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Queries PagSeguro payments and updates their status if the payment is approved
 *
 * This is needed because since 12/2023 PagSeguro is fumbling their webhooks and the first notification always has "status code: 0" in the PagSeguro's "view notifications" section
 */
class UpdatePagSeguroPaymentsTask(val m: PerfectPayments) : RunnableCoroutine {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun run() {
        logger.info { "Querying PagSeguro payments manually..." }

        val now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"))
        val endDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val initialDate = now.minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val httpResponse = PerfectPayments.http.get("https://ws.pagseguro.uol.com.br/v2/transactions?email=${m.gateway.pagSeguro.email}&token=${m.gateway.pagSeguro.token}&initialDate=$initialDate&finalDate=$endDate&page=1&maxPageResults=1000")
        val payloadAsString = httpResponse.bodyAsText()

        if (httpResponse.status.value != 200) {
            logger.warn { "Weird status code while checking for PagSeguro's payment info: ${httpResponse.status.value}; Payload: $payloadAsString" }
            return
        }

        val document = Jsoup.parse(payloadAsString)

        val transactions = document.body().select("transaction")
        logger.info { "Queried transactions: ${transactions.size}" }

        document.body().select("transaction").forEach {
            val status = it.select("status").text()
            val reference = it.select("reference").text()

            // This code is from PostPagSeguroCallbackRoute
            logger.info { "PagSeguro payment $reference status is $status" }

            val internalTransactionId = reference.split("-").last()

            val internalPayment = m.newSuspendedTransaction {
                Payment.findById(internalTransactionId.toLong())
            }

            if (internalPayment == null) {
                logger.warn { "PagSeguro Payment with Reference ID: $reference ($internalTransactionId) doesn't have a matching internal ID! Bug?" }
                return
            }

            val intStatus = status.toInt()

            val paymentStatus = PagSeguroUtils.getPaymentStatusFromPagSeguroPaymentStatus(intStatus)

            logger.info { "PagSeguro payment $reference status is $paymentStatus" }

            if (paymentStatus != null) {
                PaymentUtils.updatePaymentStatus(
                    m,
                    internalPayment,
                    paymentStatus
                )
            }
        }
    }
}