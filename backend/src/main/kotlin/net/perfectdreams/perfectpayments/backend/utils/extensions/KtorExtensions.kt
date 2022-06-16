package net.perfectdreams.perfectpayments.backend.utils.extensions

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.backend.PerfectPayments
import java.util.*

suspend fun ApplicationCall.respondJson(json: JsonElement, status: HttpStatusCode = HttpStatusCode.OK)
        = this.respondText(json.toString(), ContentType.Application.Json, status)

suspend fun ApplicationCall.respondEmptyJson(status: HttpStatusCode = HttpStatusCode.OK)
        = this.respondJson(buildJsonObject {}, status)

fun ApplicationCall.getI18nContext(m: PerfectPayments): I18nContext {
    val acceptLanguage = request.header("Accept-Language") ?: "en-US"
    val ranges = Locale.LanguageRange.parse(acceptLanguage)
    var languageToBeUsed: I18nContext? = null

    for (range in ranges) {
        // Parse the ranges until we find a language that exists, this way we can provide the language that the user likes the most, as long if it exists :)
        val language = m.languageManager.getI18nContextOrNullById(range.range.lowercase())
        if (language != null) {
            languageToBeUsed = language
            break
        }
    }

    return languageToBeUsed ?: m.languageManager.getI18nContextById(m.languageManager.defaultLanguageId)
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