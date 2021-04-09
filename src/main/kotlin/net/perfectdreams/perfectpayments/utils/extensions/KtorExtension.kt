package net.perfectdreams.perfectpayments.utils.extensions

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.*
import io.ktor.response.respondText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

/**
 * Receives the incoming content for this call as [String] using [Charsets.UTF_8]
 *
 * This is a workaround for https://youtrack.jetbrains.com/issue/KTOR-789
 *
 * @return text received from this call
 */
suspend fun ApplicationCall.receiveTextUTF8() = withContext(Dispatchers.IO) {
    receiveStream().bufferedReader(charset = Charsets.UTF_8).readText()
}