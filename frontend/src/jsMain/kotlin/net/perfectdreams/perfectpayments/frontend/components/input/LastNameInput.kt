package net.perfectdreams.perfectpayments.frontend.components.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.common.data.LastName
import net.perfectdreams.perfectpayments.frontend.components.TranslatedText
import net.perfectdreams.perfectpayments.i18n.TranslationData
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input

@Composable
fun LastNameInput(i18nContext: I18nContext, onInput: (LastName?) -> (Unit)) {
    var value by remember { mutableStateOf<LastName?>(null) }

    Div(attrs = { style { fontWeight("bold") }}) {
        TranslatedText(i18nContext, TranslationData.PersonalData.LastName)
    }
    Input(InputType.Text) {
        placeholder("Morenitta")
        onInput {
            val new = try {
                LastName(it.value)
            } catch (e: IllegalArgumentException) { null }
            onInput.invoke(new)
            value = new
        }

        if (value == null) {
            style {
                border {
                    color = rgb(255, 0, 0)
                    style = LineStyle.Solid
                    width = 1.px
                }
            }
        }
    }

    if (value == null) {
        Div(attrs = { style { color(rgb(255, 0, 0)) }}) {
            TranslatedText(
                i18nContext,
                TranslationData.InvalidPersonalData.InvalidLastName
            )
        }
    }
}