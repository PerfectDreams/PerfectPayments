package net.perfectdreams.i18nwrapper

import kotlinx.serialization.Serializable

@Serializable
class Language(
    val info: LanguageInfo,
    val textBundle: TextBundle
)