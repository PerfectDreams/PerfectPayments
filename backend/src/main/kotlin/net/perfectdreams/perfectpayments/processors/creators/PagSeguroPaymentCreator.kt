package net.perfectdreams.perfectpayments.processors.creators

import io.ktor.client.request.forms.*
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.utils.PartialPayment

class PagSeguroPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): String {
        val httpResponse = PerfectPayments.http.post<HttpResponse>("https://ws.pagseguro.uol.com.br/v2/checkout?email=${m.gateway.pagSeguro.email}&token=${m.gateway.pagSeguro.token}") {
            // Yes, it is also in the parameters, not sure why
            body = FormDataContent(Parameters.build {
                append("email", m.gateway.pagSeguro.email)
                append("token", m.gateway.pagSeguro.token)

                append("currency", "BRL")
                append("itemId1", "001")
                append("itemDescription1", partialPayment.title)
                append("itemAmount1", (partialPayment.amount.toDouble() / 100).toString())
                append("itemQuantity1", "1")
                append("reference", partialPayment.externalReference.format(paymentId))
                append("shippingAddressRequired", "false")
            })
        }

        val payload = httpResponse.readText()

        println(payload)

        // ewwww, but it works!
        val code = payload.substringAfter("<code>").substringBefore("</code>")

        return "https://pagseguro.uol.com.br/v2/checkout/payment.html?code=$code"
    }
}