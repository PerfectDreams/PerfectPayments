package net.perfectdreams.perfectpayments.utils

import com.ibm.icu.text.MessageFormat
import net.perfectdreams.i18nwrapper.Formatter
import java.util.*

class ICUFormatter(val locale: Locale) : Formatter {
    override fun format(message: String, args: Map<String, Any?>): String {
        // TODO: Cache Message Format
        return MessageFormat(message, locale).format(args)
    }
}