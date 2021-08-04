package net.perfectdreams.perfectpayments.routes.api.v1

import io.ktor.application.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.utils.extensions.respondJson
import net.perfectdreams.sequins.ktor.BaseRoute

class GetAvailableGatewaysRoute(val m: PerfectPayments) : BaseRoute("/api/v1/gateways") {
    override suspend fun onRequest(call: ApplicationCall) {
        call.respondJson(Json.encodeToJsonElement(m.config.gateways))
    }
}