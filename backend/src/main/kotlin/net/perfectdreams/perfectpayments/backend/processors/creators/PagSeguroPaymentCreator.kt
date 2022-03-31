package net.perfectdreams.perfectpayments.backend.processors.creators

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment

class PagSeguroPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): CreatedPagSeguroPaymentInfo {
        val httpResponse = PerfectPayments.http.post<HttpResponse>("https://ws.pagseguro.uol.com.br/v2/checkout?email=${m.gateway.pagSeguro.email}&token=${m.gateway.pagSeguro.token}") {
            body = FormDataContent(Parameters.build {
                // Yes, it is also in the parameters, not sure why
                append("email", m.gateway.pagSeguro.email)
                append("token", m.gateway.pagSeguro.token)

                append("currency", "BRL")
                append("itemId1", "001")
                append("itemDescription1", partialPayment.title)
                append("itemAmount1", "%.2f".format(partialPayment.amount.toDouble() / 100))
                append("itemQuantity1", "1")
                append("reference", partialPayment.externalReference.format(paymentId))
                append("shippingAddressRequired", "false")
            })
        }

        val payload = httpResponse.readText()

        println(payload)

        // ewwww, but it works!
        val code = payload.substringAfter("<code>").substringBefore("</code>")

        return CreatedPagSeguroPaymentInfo(
            code,
            "https://pagseguro.uol.com.br/v2/checkout/payment.html?code=$code"
        )
    }
}