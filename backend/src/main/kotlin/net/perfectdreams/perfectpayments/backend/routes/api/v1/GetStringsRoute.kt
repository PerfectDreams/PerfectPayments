package net.perfectdreams.perfectpayments.backend.routes.api.v1

import io.ktor.application.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.utils.extensions.getI18nContext
import net.perfectdreams.perfectpayments.backend.utils.extensions.respondJson
import net.perfectdreams.sequins.ktor.BaseRoute

class GetStringsRoute(val m: PerfectPayments) : BaseRoute("/api/v1/strings") {
    override suspend fun onRequest(call: ApplicationCall) {
        call.respondJson(
            Json.encodeToJsonElement(
                call.getI18nContext(m)
                    .language
            )
        )
    }
}