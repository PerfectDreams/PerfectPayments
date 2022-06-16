package net.perfectdreams.perfectpayments.backend.routes.api.v1.payments

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.backend.utils.PartialPayment
import net.perfectdreams.perfectpayments.backend.utils.extensions.receiveTextUTF8
import net.perfectdreams.perfectpayments.common.data.api.CreatePaymentRequest
import net.perfectdreams.perfectpayments.common.data.api.CreatePaymentResponse
import java.util.*

class PostCreatePaymentRoute(m: PerfectPayments) : RequiresAPIAuthenticationRoute(m, "/api/v1/payments") {
    override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
        val paymentPayload = call.receiveTextUTF8()

        val request = Json.decodeFromString<CreatePaymentRequest>(paymentPayload)

        val paymentTitle = request.title
        val currencyId = request.currencyId
        val amount = request.amount
        val callbackUrl = request.callbackUrl
        val externalReference = request.externalReference

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
            Json.encodeToString(
                CreatePaymentResponse(
                    partialPaymentId.toString(),
                    "${m.config.website.url}/checkout/$partialPaymentId"
                )
            ),
            ContentType.Application.Json
        )
    }
}