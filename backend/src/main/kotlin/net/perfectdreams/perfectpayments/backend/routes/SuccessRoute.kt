package net.perfectdreams.perfectpayments.backend.routes

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.response.respondText
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import net.perfectdreams.perfectpayments.backend.html.SuccessView
import net.perfectdreams.perfectpayments.backend.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class SuccessRoute(val m: PerfectPayments) : BaseRoute("/success") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(SuccessView(locale).render(), ContentType.Text.Html)
    }
}