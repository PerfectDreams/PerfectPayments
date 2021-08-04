package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import net.perfectdreams.i18nwrapper.I18nContext
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.i18n.TranslationKeys
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text

@Composable
fun PaymentHeader(
    i18nContext: I18nContext,
    stageTitle: String,
    payment: ClientSidePartialPayment
) {
    Div({ id("payment-header") }) {
        Div {
            H1 {
                Text(stageTitle)
            }
        }

        Div({ classes("divisor") })

        Div {
            Div {
                B {
                    Text("${i18nContext.get(TranslationKeys.Header.Product)}: ")
                }

                Text(payment.title)
            }

            Div {
                B {
                    Text("${i18nContext.get(TranslationKeys.Header.Value)}: ")
                }

                Text("${payment.amount.toDouble() / 100} ${payment.currencyId}")
            }
        }
    }
}