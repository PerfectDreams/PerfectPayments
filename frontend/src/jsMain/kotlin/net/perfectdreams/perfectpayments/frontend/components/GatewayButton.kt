package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.TranslationData
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun GatewayButton(
    m: PerfectPaymentsFrontend,
    i18nContext: I18nContext,
    gateway: PaymentGateway
) {
    Div(
        {
            classes("payment-button")

            onClick {
                m.delegatedScreenState = Screen.NotaFiscalRequest(gateway)
            }
        },
    ) {
        Img(
            src = "${window.location.origin}/${gateway.imageUrl}", // TODO: Fix
            alt = "gateways.${gateway.name.lowercase()}.name",
            attrs = { style { width(100.percent); maxHeight("80px") } }
        )

        Span(attrs = { style { display(DisplayStyle.Block) } }) {
            B {
                TranslatedText(i18nContext, TranslationData.SelectPaymentGateway.Accepts)
                Text(":")
            }

            val acceptsMethods = gateway.methods.map {
                it.name.lowercase()
                    .replace("_", " ")
                    .split(" ")
                    .map {
                        it.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                    .joinToString("")
                    .toCharArray().apply {
                        this[0] = this[0].toLowerCase()
                    }
                    .joinToString("")
            }.map { i18nContext.get("methods.$it") }.joinToString(", ")

            Text(" $acceptsMethods")
        }
    }
}