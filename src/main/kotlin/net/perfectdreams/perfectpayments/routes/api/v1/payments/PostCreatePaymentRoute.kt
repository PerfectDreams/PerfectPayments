package net.perfectdreams.perfectpayments.routes.api.v1.payments

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import kotlinx.serialization.json.*
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.utils.PartialPayment
import net.perfectdreams.sequins.ktor.BaseRoute
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