package net.perfectdreams.i18nwrapper

import kotlinx.serialization.Serializable

@Serializable
data class LanguageInfo(
    val name: String,
    val inheritsFrom: String? = null,
    val formattingLanguageId: String
)