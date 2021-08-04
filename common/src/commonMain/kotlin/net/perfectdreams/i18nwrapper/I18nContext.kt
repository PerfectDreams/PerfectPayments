package net.perfectdreams.i18nwrapper

import mu.KotlinLogging
import net.perfectdreams.i18nwrapper.keydata.ListTranslationData
import net.perfectdreams.i18nwrapper.keydata.StringTranslationData
import net.perfectdreams.i18nwrapper.keys.ListTranslationKey
import net.perfectdreams.i18nwrapper.keys.StringTranslationKey

class I18nContext(
    val formatter: Formatter,
    val language: Language
) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun get(key: StringTranslationKey, arguments: MutableMap<String, Any?>.() -> Unit) = get(key, buildMap(arguments))
    fun get(key: StringTranslationKey, arguments: Map<String, Any?> = mapOf()) = get(key.key, arguments)
    @OptIn(ExperimentalStdlibApi::class)
    fun get(key: String, arguments: MutableMap<String, Any?>.() -> Unit) = get(key, buildMap(arguments))
    fun get(key: StringTranslationData) = get(key.key.key, key.arguments)

    fun get(key: String, arguments: Map<String, Any?> = mapOf()): String {
        try {
            val content = language.textBundle.strings[key] ?: throw RuntimeException("Key $key doesn't exist in locale!")
            return formatter.format(content, replaceKeysWithMessage(arguments))
        } catch (e: RuntimeException) {
            logger.error(e) { "Error when trying to retrieve $key for locale" }
        }
        return "!!{$key}!!"
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun get(key: ListTranslationKey, arguments: MutableMap<String, Any?>.() -> Unit) = get(key, buildMap(arguments))
    fun get(key: ListTranslationKey, arguments: Map<String, Any?> = mapOf()) = getList(key.key, arguments)
    fun get(key: ListTranslationData) = getList(key.key.key, key.arguments)
    @OptIn(ExperimentalStdlibApi::class)
    fun getList(key: String, arguments: MutableMap<String, Any?>.() -> Unit) = getList(key, buildMap(arguments))

    fun getList(key: String, arguments: Map<String, Any?> = mapOf()): List<String> {
        try {
            val list = language.textBundle.lists[key] ?: throw RuntimeException("Key $key doesn't exist in locale!")
            val newArgs = replaceKeysWithMessage(arguments)
            return list.map { formatter.format(it, newArgs) }
        } catch (e: RuntimeException) {
            logger.error(e) { "Error when trying to retrieve $key for locale" }
        }
        return listOf("!!{$key}!!")
    }

    private fun replaceKeysWithMessage(map: Map<String, Any?>): MutableMap<String, Any?> {
        val newMap = mutableMapOf<String, Any?>()

        for ((key, value) in map) {
            when (value) {
                is StringTranslationKey -> {
                    // We will use a copy of the current map but without the "key", to avoid recursion issues
                    newMap[key] = get(value, map.toMutableMap().apply { this.remove(key) })
                }
                is StringTranslationData -> {
                    // We will use a copy of the current map but without the "key", to avoid recursion issues
                    newMap[key] = get(value)
                }
                else -> {
                    newMap[key] = value
                }
            }
        }

        return newMap
    }
}