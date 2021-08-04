package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.i18nwrapper.keydata.StringTranslationData
import org.jetbrains.compose.web.dom.Text

@Composable
fun TranslatedText(i18nContext: I18nContext, data: StringTranslationData) {
    Text(i18nContext.get(data))
}