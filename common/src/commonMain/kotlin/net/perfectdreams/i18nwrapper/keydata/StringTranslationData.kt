package net.perfectdreams.i18nwrapper.keydata

import net.perfectdreams.i18nwrapper.keys.StringTranslationKey

open class StringTranslationData(
    val key: StringTranslationKey,
    val arguments: Map<String, Any?>
)