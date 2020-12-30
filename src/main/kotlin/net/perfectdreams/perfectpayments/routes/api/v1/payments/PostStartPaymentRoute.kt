package net.perfectdreams.perfectpayments.routes.api.v1.payments

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.request.receiveText
import io.ktor.response.respondText
import kotlinx.serialization.json.*
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.payments.PaymentGateway
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import net.perfectdreams.perfectpayments.routes.api.v1.RequiresAPIAuthenticationRoute
import net.perfectdreams.perfectpayments.tables.Payments
import net.perfectdreams.perfectpayments.utils.PartialPayment
import net.perfectdreams.perfectpayments.utils.PaymentQuery
import net.perfectdreams.sequins.ktor.BaseRoute
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

class PostStartPaymentRoute(m: PerfectPayments) : RequiresAPIAuthenticationRoute(m, "/api/v1/payments/{partialPaymentId}") {
    override suspend fun onAuthenticatedRequest(call: ApplicationCall) {
        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])
        val partialPayment = m.partialPayments[partialPaymentId] ?: throw RuntimeException("Missing partial payment")

        val paymentPayload = call.receiveText()

        val jsonBody = Json.parseToJsonElement(paymentPayload)
            .jsonObject

        val data = jsonBody["data"]!!.jsonObject

        val gateway = PaymentGateway.valueOf(jsonBody["gateway"]!!.jsonPrimitive.content)

        val paymentUrl = PaymentQuery.startPayment(
            m,
            partialPaymentId,
            partialPayment,
            gateway,
            data
        )

        call.respondText(
            buildJsonObject {
                put("paymentUrl", paymentUrl)
            }.toString(),
            ContentType.Application.Json
        )
    }
}