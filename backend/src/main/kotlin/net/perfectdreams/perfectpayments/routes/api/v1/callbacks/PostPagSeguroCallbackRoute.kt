package net.perfectdreams.perfectpayments.routes.api.v1.callbacks

import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.request.*
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.respondEmptyJson
import net.perfectdreams.sequins.ktor.BaseRoute
import org.jsoup.Jsoup
import java.util.*

class PostPagSeguroCallbackRoute(val m: PerfectPayments) : BaseRoute("/api/v1/callbacks/pagseguro") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        logger.info { "Received PagSeguro Webhook Request" }

        val authorization = call.request.header("Authorization")

        if (authorization == null || !authorization.startsWith("Basic ")) {
            logger.warn { "Request Authorization is different than what it is expected or null! Received Seller Token: $authorization"}
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val rawBase64Authorization = authorization.substringAfter("Basic ")

        val joinedCredentials = Base64.getDecoder().decode(rawBase64Authorization).toString(Charsets.UTF_8)
        val username = joinedCredentials.substringBefore(":")
        val password = joinedCredentials.substringAfter(":")

        if (username != m.gateway.pagSeguro.notificationUsername || password != m.gateway.pagSeguro.notificationPassword) {
            logger.warn { "Request Authorization authorization doesn't match! Received Username: $username; Received Password: $password"}
            call.respondEmptyJson(HttpStatusCode.Forbidden)
            return
        }

        val parameters = call.receiveParameters()
        val notificationCode = parameters["notificationCode"]
        val notificationType = parameters["notificationType"]

        if (notificationCode == null) {
            logger.info { "Received notification without a notificationCode, notificationType is $notificationType" }
            call.respondEmptyJson()
            return
        }

        logger.info { "Received notification about \"$notificationCode\", notificationType is $notificationType" }

        if (notificationType == "transaction") {
            val httpResponse =
                PerfectPayments.http.get<HttpResponse>("https://ws.pagseguro.uol.com.br/v3/transactions/notifications/$notificationCode?email=${m.gateway.pagSeguro.email}&token=${m.gateway.pagSeguro.token}")

            val payloadAsString = httpResponse.readText()

            if (httpResponse.status.value != 200) {
                logger.warn { "Weird status code while checking for PagSeguro's payment info: ${httpResponse.status.value}; Payload: $payloadAsString" }
                call.respondEmptyJson()
                return
            }

            val jsoup = Jsoup.parse(payloadAsString)

            val reference = jsoup.getElementsByTag("reference").first().text()
            val status = jsoup.getElementsByTag("status").first().text()

            logger.info { "PagSeguro payment $reference status is $status" }

            val internalTransactionId = reference.split("-").last()

            val internalPayment = m.newSuspendedTransaction {
                Payment.findById(internalTransactionId.toLong())
            }

            if (internalPayment == null) {
                logger.warn { "PagSeguro Payment with Reference ID: $reference ($internalTransactionId) doesn't have a matching internal ID! Bug?" }
                call.respondEmptyJson()
                return
            }

            val intStatus = status.toInt()

            val paymentStatus = when (intStatus) {
                3, 4 -> PaymentStatus.APPROVED
                5, 9 -> PaymentStatus.CHARGED_BACK
                else -> null
            }

            logger.info { "PagSeguro payment $reference status is $paymentStatus" }

            if (paymentStatus != null) {
                m.newSuspendedTransaction {
                    // https://dev.pagseguro.uol.com.br/docs/api-notificacao-v1
                    internalPayment.status = paymentStatus
                }
            }

            if (paymentStatus == PaymentStatus.APPROVED) {
                if (internalPayment.paidAt != null) {
                    logger.warn { "PagSeguro Payment with Reference ID: $reference ($internalTransactionId) is already paid! Ignoring..." }
                    call.respondEmptyJson()
                    return
                }

                logger.info { "Setting Payment $internalTransactionId as paid! (via PagSeguro payment $reference)" }

                m.newSuspendedTransaction {
                    // Pagamento aprovado!
                    internalPayment.paidAt = System.currentTimeMillis()
                }

                m.notaFiscais?.generateNotaFiscal(internalPayment)
            }

            // Send a update to the callback URL
            PaymentQuery.sendPaymentNotification(m, internalPayment)
        }

        call.respondEmptyJson()
    }
}