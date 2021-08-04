package net.perfectdreams.perfectpayments.frontend

import net.perfectdreams.i18nwrapper.Formatter

class NoopFormatter : Formatter {
    override fun format(message: String, args: Map<String, Any?>): String {
        return message
    }
}