package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.data.ClientSidePartialPayment
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentMethod
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentMethodSelection
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.i18n.I18nKeys
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Hr

@Composable
fun SelectUserFacingPaymentMethod(
    m: PerfectPaymentsFrontend,
    i18nContext: I18nContext,
    partialPaymentData: ClientSidePartialPayment,
    selections: List<UserFacingPaymentMethodSelection>
) {
    PaymentHeader(i18nContext, i18nContext.get(I18nKeys.SelectPaymentGateway.Title), partialPaymentData)

    Div({ id("wrapper") }) {
        Div({ id("payment-methods") }) {
            Div({ id("payment-method-list") }) {
                val availableSelections = selections.filter { it is UserFacingPaymentMethod && it.gateway in m.availableGateways!! }

                for ((index, userFacingPaymentSelection) in availableSelections.withIndex()) {
                    UserFacingPaymentButton(m, i18nContext, userFacingPaymentSelection)

                    val isLast = index == availableSelections.size - 1

                    if (!isLast) {
                        // Needs to be in a div to not require width: 100%
                        Div {
                            Hr {}
                        }
                    }
                }
            }
        }
    }
}