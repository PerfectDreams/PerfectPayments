package net.perfectdreams.perfectpayments.frontend.components.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.data.FirstName
import net.perfectdreams.perfectpayments.frontend.components.TranslatedText
import net.perfectdreams.perfectpayments.i18n.I18nKeysData
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.fontWeight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.rgb
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input

@Composable
fun FirstNameInput(i18nContext: I18nContext, onInput: (FirstName?) -> (Unit)) {
    var value by remember { mutableStateOf<FirstName?>(null) }

    Div(attrs = { style { fontWeight("bold") }}) {
        TranslatedText(i18nContext, I18nKeysData.PersonalData.FirstName)
    }
    Input(InputType.Text) {
        placeholder("Loritta")
        onInput {
            val new = try {
                FirstName(it.value)
            } catch (e: IllegalArgumentException) { null }
            onInput.invoke(new)
            value = new
        }

        style {
            width(100.percent)

            if (value == null) {
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
                I18nKeysData.InvalidPersonalData.InvalidFirstName
            )
        }
    }
}