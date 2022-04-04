package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.I18nKeys
import net.perfectdreams.perfectpayments.i18n.I18nKeysData
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun ParentalWarning(m: PerfectPaymentsFrontend, i18nContext: I18nContext, partialPaymentData: ClientSidePartialPayment, screen: Screen.ParentalWarningRequest) {
    PaymentHeader(i18nContext, i18nContext.get(I18nKeys.ParentalWarning.Title), partialPaymentData)

    Div({ id("wrapper") }) {
        Div({ style { textAlign("center") }}) {
            Img(
                src = "${window.location.origin}/assets/img/lori_policial.png",
                attrs = {
                    style {
                        height(300.px)
                        maxWidth(100.percent)
                        property("aspect-ratio", "165/303") // Used to avoid content shifting due to image load
                    }
                }
            )
        }
        i18nContext.get(I18nKeysData.ParentalWarning.Description).forEach {
            P {
                Text(it)
            }
        }

        Hr {}

        Div(attrs = { style { display(DisplayStyle.Flex); width(100.percent); gap(1.em) } }) {
            Button(attrs = {
                classes("button", "red")
                style { width(100.percent) }

                onClick {
                    window.location.href = "https://random.cat"
                }
            }) {
                Text(i18nContext.get(I18nKeysData.ParentalWarning.SorryLoriPleaseDontHurtMe))
            }

            Button(attrs = {
                classes("button", "primary")
                if (screen.countdown != 0)
                    disabled()
                style { width(100.percent) }

                if (screen.countdown == 0)
                    onClick {
                        m.switch(Screen.NotaFiscalRequest(screen.gateway))
                    }
            }) {
                if (screen.countdown == 0)
                    Text(i18nContext.get(I18nKeysData.ParentalWarning.IHavePermission))
                else
                    Text(i18nContext.get(I18nKeysData.ParentalWarning.IHavePermission) + " (${screen.countdown}s)")
            }
        }
    }
}