package net.perfectdreams.perfectpayments.backend.processors.creators

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment
import net.perfectdreams.perfectpayments.backend.utils.TextUtils

class PagSeguroPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): CreatedPagSeguroPaymentInfo {
        // I've tried using PagSeguro's JSON API, but it doesn't work, it always throws an error where the server wasn't able to accept your request, even if I copied the example from
        // PagSeguro's website!
        val httpResponse = PerfectPayments.http.post("https://ws.pagseguro.uol.com.br/v2/checkout?email=${m.gateway.pagSeguro.email}&token=${m.gateway.pagSeguro.token}") {
            setBody(
                FormDataContent(Parameters.build {
                    // Yes, it is also in the parameters, not sure why
                    append("email", m.gateway.pagSeguro.email)
                    append("token", m.gateway.pagSeguro.token)

                    append("currency", "BRL")
                    append("itemId1", "001")
                    append("itemDescription1", TextUtils.cleanTitle(partialPayment.title))
                    append("itemAmount1", "%.2f".format(partialPayment.amount.toDouble() / 100))
                    append("itemQuantity1", "1")
                    append("reference", partialPayment.externalReference.format(paymentId))
                    append("shippingAddressRequired", "false")
                })
            )
        }

        val payload = httpResponse.bodyAsText()

        println(payload)

        // ewwww, but it works!
        val code = payload.substringAfter("<code>").substringBefore("</code>")

        return CreatedPagSeguroPaymentInfo(
            code,
            "https://pagseguro.uol.com.br/v2/checkout/payment.html?code=$code"
        )
    }
}