package net.perfectdreams.i18nwrapper

interface Formatter {
    fun format(message: String, args: Map<String, Any?>): String
}