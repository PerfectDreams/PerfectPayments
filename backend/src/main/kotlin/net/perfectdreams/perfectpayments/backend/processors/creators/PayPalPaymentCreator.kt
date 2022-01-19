package net.perfectdreams.perfectpayments.backend.processors.creators

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment
import java.util.*

class PayPalPaymentCreator(val m: PerfectPayments) : PaymentCreator {
    override suspend fun createPayment(paymentId: Long, partialPayment: PartialPayment, data: JsonObject): CreatedPaymentInfo {
        val payload = PerfectPayments.http.post<HttpResponse>(m.gateway.payPal.getBaseUrl() + "/v2/checkout/orders") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Basic ${Base64.getEncoder().encodeToString("${m.gateway.payPal.clientId}:${m.gateway.payPal.clientSecret}".toByteArray())}")

            body = buildJsonObject {
                put("intent", "CAPTURE")
                putJsonArray("purchase_units") {
                    addJsonObject {
                        putJsonObject("amount") {
                            put("currency_code", "BRL")
                            put("value", (partialPayment.amount.toDouble() / 100))
                        }
                        put("custom_id", partialPayment.externalReference.format(paymentId))
                    }
                }
                putJsonObject("application_context") {
                    put("brand_name", "PerfectPayments")
                    put("locale", "pt-BR")
                    put("shipping_preference", "NO_SHIPPING")
                    put("user_action", "PAY_NOW")
                    put("return_url", m.config.website.url + "/success")
                    put("cancel_url", m.config.website.url + "/cancelled")
                }
            }.toString()
        }

        val element = Json.parseToJsonElement(payload.readText())
            .jsonObject

        val links = element["links"]!!.jsonArray

        val link = links.first { it.jsonObject["rel"]!!.jsonPrimitive.content == "approve" }
            .jsonObject["href"]!!.jsonPrimitive.content

        return CreatedPayPalPaymentInfo(
            element["id"]!!.jsonPrimitive.content,
            link
        )
    }
}