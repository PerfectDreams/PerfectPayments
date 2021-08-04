package net.perfectdreams.perfectpayments.frontend

import IntlMessageFormat
import net.perfectdreams.i18nwrapper.Formatter
import org.jetbrains.compose.web.css.jsObject

class IntlMFFormatter : Formatter {
    override fun format(message: String, args: Map<String, Any?>): String {
        // TODO: Cache
        val mf = IntlMessageFormat(message, "en-US")

        return mf.format(
            jsObject {
                for (arg in args) {
                    this[arg.key] = arg.value
                }
            }
        )
    }
}