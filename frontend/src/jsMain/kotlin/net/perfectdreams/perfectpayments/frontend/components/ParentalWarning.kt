package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.I18nKeys
import net.perfectdreams.perfectpayments.i18n.I18nKeysData
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

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
                style { width(100.percent) }

                onClick {
                    m.switch(Screen.NotaFiscalRequest(screen.gateway))
                }
            }) {
                Text(i18nContext.get(I18nKeysData.ParentalWarning.IHavePermission))
            }
        }
    }
}