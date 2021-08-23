package net.perfectdreams.perfectpayments.backend.processors.creators

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment

class PicPayPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): String {
        val buyer = data["buyer"]!!.jsonObject

        val firstName = buyer["firstName"]!!.jsonPrimitive.content
        val lastName = buyer["lastName"]!!.jsonPrimitive.content
        val document = buyer["document"]!!.jsonPrimitive.content
        val email = buyer["email"]!!.jsonPrimitive.content
        val phone = buyer["phone"]!!.jsonPrimitive.content

        val jsonOwo = buildJsonObject {
            put("referenceId", partialPayment.externalReference.format(paymentId))
            put("callbackUrl", "https://payments.perfectdreams.net/api/v1/callbacks/picpay")
            put("value", (partialPayment.amount.toDouble() / 100))
            putJsonObject("buyer") {
                put("firstName", firstName)
                put("lastName", lastName)
                put("document", document)
                put("email", email)
                put("phone", phone)
            }
        }.toString()

        println(jsonOwo)

        val httpResponse = PerfectPayments.http.post<HttpResponse>("https://appws.picpay.com/ecommerce/public/payments") {
            contentType(ContentType.Application.Json)
            header("x-picpay-token", m.gateway.picPay.token)

            body = jsonOwo
        }

        val payload = httpResponse.readText()

        println(payload)

        val picPayJson = Json.parseToJsonElement(payload)
            .jsonObject

        return picPayJson["paymentUrl"]!!.jsonPrimitive.content
    }
}