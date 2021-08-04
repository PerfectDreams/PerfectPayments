package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.TranslationKeys
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.gap
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
fun NotaFiscalRequest(m: PerfectPaymentsFrontend, i18nContext: I18nContext, partialPaymentData: ClientSidePartialPayment, screen: Screen.NotaFiscalRequest) {
    PaymentHeader(i18nContext, i18nContext.get(TranslationKeys.TaxInvoice.Title), partialPaymentData)

    Div({ id("wrapper") }) {
        Div({ style { textAlign("center") }}) {
            Img(
                src = "${window.location.origin}/assets/img/lori_nota_fiscal.png",
                attrs = { style { width(300.px); maxWidth(100.percent) }})
        }
        i18nContext.get(TranslationKeys.TaxInvoice.Description).forEach {
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
                    m.delegatedScreenState = if (screen.gateway == PaymentGateway.PICPAY) {
                        Screen.PicPayDataCollectRequest(
                            screen.gateway,
                            null
                        )
                    } else {
                        Screen.DataCollected(screen.gateway, null)
                    }
                }
            }) {
                Text(i18nContext.get(TranslationKeys.TaxInvoice.DoNotInclude))
            }

            Button(attrs = {
                classes("button", "primary")
                style { width(100.percent) }

                onClick {
                    m.delegatedScreenState = Screen.NotaFiscalDataCollectRequest(screen.gateway)
                }
            }) {
                Text(i18nContext.get(TranslationKeys.TaxInvoice.Include))
            }
        }
    }
}