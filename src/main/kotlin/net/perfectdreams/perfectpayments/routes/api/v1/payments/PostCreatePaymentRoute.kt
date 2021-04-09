package net.perfectdreams.perfectpayments.routes.api.v1.payments

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.utils.PartialPayment
import java.util.*

class PostCreatePaymentRoute(m: PerfectPayments) : RequiresAPIAuthenticationRoute(m, "/api/v1/payments") {
    override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
        val paymentPayload = call.receiveText()

        val body = Json.parseToJsonElement(paymentPayload)
            .jsonObject

        val paymentTitle = body["title"]!!.jsonPrimitive.content
        val currencyId = body["currencyId"]!!.jsonPrimitive.content
        val amount = body["amount"]!!.jsonPrimitive.long
        val callbackUrl = body["callbackUrl"]!!.jsonPrimitive.content
        val externalReference = body["externalReference"]!!.jsonPrimitive.content

        val partialPaymentId = UUID.randomUUID()
        val partialPayment = PartialPayment(
            paymentTitle,
            amount,
            currencyId,
            callbackUrl,
            externalReference
        )

        m.partialPayments[partialPaymentId] = partialPayment

        // After generating the payment, we will return the UUID of the payment + payment URL
        call.respondText(
            buildJsonObject {
                put("id", partialPaymentId.toString())
                put(
                    "paymentUrl",
                    "${m.config.website.url}/checkout/$partialPaymentId"
                )
            }.toString(),
            ContentType.Application.Json
        )
    }
}