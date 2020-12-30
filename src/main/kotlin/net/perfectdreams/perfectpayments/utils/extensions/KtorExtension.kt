package net.perfectdreams.perfectpayments.utils.extensions

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respondText
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import net.perfectdreams.loritta.utils.locale.BaseLocale
import net.perfectdreams.perfectpayments.PerfectPayments
import java.util.*

suspend fun ApplicationCall.respondJson(json: JsonElement, status: HttpStatusCode = HttpStatusCode.OK)
        = this.respondText(json.toString(), ContentType.Application.Json, status)

suspend fun ApplicationCall.respondEmptyJson(status: HttpStatusCode = HttpStatusCode.OK)
        = this.respondJson(buildJsonObject {}, status)

suspend fun ApplicationCall.getLocale(m: PerfectPayments): BaseLocale {
    val acceptLanguage = request.header("Accept-Language") ?: "en-US"
    val ranges = Locale.LanguageRange.parse(acceptLanguage).reversed()
    var localeId = "en-us"
    for (range in ranges) {
        localeId = range.range.toLowerCase()
        if (localeId == "pt-br" || localeId == "pt") {
            localeId = "default"
        }
        if (localeId == "en") {
            localeId = "en-us"
        }
    }

    return m.getLocaleById(localeId)
}