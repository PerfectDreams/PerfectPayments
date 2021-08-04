package net.perfectdreams.perfectpayments.routes

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
import io.ktor.response.respondText
import net.perfectdreams.perfectpayments.PerfectPayments
import net.perfectdreams.perfectpayments.html.SuccessView
import net.perfectdreams.perfectpayments.utils.extensions.getI18nContext
import net.perfectdreams.sequins.ktor.BaseRoute

class SuccessRoute(val m: PerfectPayments) : BaseRoute("/success") {
    override suspend fun onRequest(call: ApplicationCall) {
        val locale = call.getI18nContext(m)
        call.respondText(SuccessView(locale).render(), ContentType.Text.Html)
    }
}