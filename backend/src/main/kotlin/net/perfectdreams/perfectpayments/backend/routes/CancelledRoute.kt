package net.perfectdreams.perfectpayments.backend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.html.CancelledView
import net.perfectdreams.perfectpayments.backend.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class CancelledRoute(val m: PerfectPayments) : BaseRoute("/cancelled") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(CancelledView(locale, m.hashManager).render(), ContentType.Text.Html)
    }
}