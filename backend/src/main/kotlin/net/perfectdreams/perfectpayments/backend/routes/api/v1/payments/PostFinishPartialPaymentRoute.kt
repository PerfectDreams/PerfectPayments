package net.perfectdreams.perfectpayments.backend.routes.api.v1.payments

import io.ktor.server.application.*
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.dao.Payment
import net.perfectdreams.perfectpayments.backend.payments.PaymentStatus
import net.perfectdreams.perfectpayments.backend.processors.creators.CreatedPaymentInfoWithUrl
import net.perfectdreams.perfectpayments.backend.processors.creators.CreatedSandboxPaymentInfo
import net.perfectdreams.perfectpayments.backend.utils.PaymentQuery
import net.perfectdreams.perfectpayments.backend.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondJson
import net.perfectdreams.perfectpayments.common.data.FilledPartialPayment
import net.perfectdreams.perfectpayments.common.data.PaymentCreatedResponse
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class PostFinishPartialPaymentRoute(val m: PerfectPayments) : BaseRoute("/api/v1/partial-payments/{partialPaymentId}/finish") {
    override suspend fun onRequest(call: ApplicationCall) {
        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])
        val partialPayment = m.partialPayments[partialPaymentId]!!
        
        val paymentPayload = call.receiveTextUTF8()

        val filledPartialPayment = Json.decodeFromString<FilledPartialPayment>(paymentPayload)

        if (filledPartialPayment.gateway !in m.config.gateways)
            throw IllegalArgumentException("Gateway ${filledPartialPayment.gateway} is not enabled!")

        when (filledPartialPayment.gateway) {
            PaymentGateway.SANDBOX -> {
                // SANDBOX MODE!
                // Start the payment
                val createdPaymentData = PaymentQuery.startPayment(
                    m,
                    partialPaymentId,
                    partialPayment,
                    filledPartialPayment.gateway,
                    filledPartialPayment.personalData,
                    buildJsonObject {}
                ) as CreatedSandboxPaymentInfo

                val internalPayment = m.newSuspendedTransaction {
                    Payment.findById(createdPaymentData.id.toLong())!!
                }

                // Force the payment as complete
                m.newSuspendedTransaction {
                    // Pagamento aprovado!
                    internalPayment.paidAt = Clock.System.now()
                    internalPayment.status = PaymentStatus.APPROVED
                    internalPayment.netReceivedAmount = internalPayment.amount
                }

                // Send a update to the callback URL
                PaymentQuery.sendPaymentNotification(m, internalPayment)

                // Generate nota fiscal
                m.notaFiscais?.generateNotaFiscal(internalPayment, null)

                // Success!
                call.respondJson(
                    Json.encodeToJsonElement(PaymentCreatedResponse("${m.config.website.url}/success?sandbox"))
                )
            }
            PaymentGateway.PICPAY -> {
                val picPayPersonalData = filledPartialPayment.picPayPersonalData!!

                val createdPaymentData = PaymentQuery.startPayment(
                    m,
                    partialPaymentId,
                    partialPayment,
                    filledPartialPayment.gateway,
                    filledPartialPayment.personalData,
                    buildJsonObject {
                        putJsonObject("buyer") {
                            put("firstName", picPayPersonalData.firstName.name)
                            put("lastName", picPayPersonalData.lastName.name)
                            put("document", picPayPersonalData.socialNumber.cleanDocument)
                            put("email", picPayPersonalData.email.buildEmailAddress())
                            put("phone", picPayPersonalData.phone.phoneNumber)
                        }
                    }
                )

                if (createdPaymentData !is CreatedPaymentInfoWithUrl)
                    error("I don't know how to handle $createdPaymentData!")

                call.respondJson(
                    Json.encodeToJsonElement(PaymentCreatedResponse(createdPaymentData.url))
                )
            }
            else -> {
                val createdPaymentData = PaymentQuery.startPayment(
                    m,
                    partialPaymentId,
                    partialPayment,
                    filledPartialPayment.gateway,
                    filledPartialPayment.personalData,
                    buildJsonObject {}
                )

                if (createdPaymentData !is CreatedPaymentInfoWithUrl)
                    error("I don't know how to handle $createdPaymentData!")

                call.respondJson(
                    Json.encodeToJsonElement(PaymentCreatedResponse(createdPaymentData.url))
                )
            }
        }
    }
}