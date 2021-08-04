package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.i18n.TranslationKeys
import org.jetbrains.compose.web.dom.Div

@Composable
fun SelectGateway(m: PerfectPaymentsFrontend, i18nContext: I18nContext, partialPaymentData: ClientSidePartialPayment) {
    PaymentHeader(i18nContext, i18nContext.get(TranslationKeys.SelectPaymentGateway.Title), partialPaymentData)

    Div({ id("wrapper") }) {
        Div({ id("payment-methods") }) {
            for (gateway in m.availableGateways!!) {
                GatewayButton(m, i18nContext, gateway)
            }
        }
    }
}