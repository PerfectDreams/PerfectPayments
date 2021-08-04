package net.perfectdreams.perfectpayments.routes.api.v1.payments

import io.ktor.application.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.common.data.FilledPartialPayment
import net.perfectdreams.perfectpayments.common.data.PaymentCreatedResponse
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.dao.Payment
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.perfectpayments.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.utils.extensions.respondJson
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
                val paymentId = PaymentQuery.startPayment(
                    m,
                    partialPaymentId,
                    partialPayment,
                    filledPartialPayment.gateway,
                    filledPartialPayment.personalData,
                    buildJsonObject {}
                )

                val internalPayment = m.newSuspendedTransaction {
                    Payment.findById(paymentId.toLong())!!
                }

                // Force the payment as complete
                m.newSuspendedTransaction {
                    // Pagamento aprovado!
                    internalPayment.paidAt = System.currentTimeMillis()
                    internalPayment.status = PaymentStatus.APPROVED
                }

                // Generate nota fiscal
                m.notaFiscais?.generateNotaFiscal(internalPayment)

                // Success!
                call.respondJson(
                    Json.encodeToJsonElement(PaymentCreatedResponse("${m.config.website.url}/success?sandbox"))
                )
            }
            PaymentGateway.PICPAY -> {
                val picPayPersonalData = filledPartialPayment.picPayPersonalData!!

                val paymentUrl = PaymentQuery.startPayment(
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

                call.respondJson(
                    Json.encodeToJsonElement(PaymentCreatedResponse(paymentUrl))
                )
            }
            else -> {
                val paymentUrl = PaymentQuery.startPayment(
                    m,
                    partialPaymentId,
                    partialPayment,
                    filledPartialPayment.gateway,
                    filledPartialPayment.personalData,
                    buildJsonObject {}
                )

                call.respondJson(
                    Json.encodeToJsonElement(PaymentCreatedResponse(paymentUrl))
                )
            }
        }
    }
}