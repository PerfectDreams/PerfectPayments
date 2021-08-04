package net.perfectdreams.perfectpayments.backend.routes.api.v1.payments

import io.ktor.application.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import mu.KotlinLogging
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondEmptyJson
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondJson
import net.perfectdreams.sequins.ktor.BaseRoute
import java.util.*

class GetPartialPaymentInfoRoute(val m: PerfectPayments) : BaseRoute("/api/v1/partial-payments/{partialPaymentId}") {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun onRequest(call: ApplicationCall) {
        val partialPaymentId = UUID.fromString(call.parameters["partialPaymentId"])
        val partialPayment = m.partialPayments[partialPaymentId]

        if (partialPayment == null)
            call.respondEmptyJson(HttpStatusCode.NotFound)
        else
            return call.respondJson(
                Json.encodeToJsonElement(
                    ClientSidePartialPayment(
                        partialPayment.title,
                        partialPayment.amount,
                        partialPayment.currencyId
                    )
                )
            )
    }
}