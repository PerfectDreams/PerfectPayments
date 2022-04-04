package net.perfectdreams.perfectpayments.frontend.components

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.perfectpayments.common.payments.PaymentMethodCountry
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentGroup
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentMethod
import net.perfectdreams.perfectpayments.common.payments.UserFacingPaymentMethodSelection
import net.perfectdreams.perfectpayments.frontend.PerfectPaymentsFrontend
import net.perfectdreams.perfectpayments.frontend.screen.Screen
import net.perfectdreams.perfectpayments.i18n.I18nKeys
import net.perfectdreams.perfectpayments.i18n.I18nKeysData
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.em
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.I
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

@Composable
fun UserFacingPaymentButton(
    m: PerfectPaymentsFrontend,
    i18nContext: I18nContext,
    userFacingPaymentMethod: UserFacingPaymentMethodSelection
) {
    Div(
        {
            classes("method-button")

            onClick {
                when (userFacingPaymentMethod) {
                    is UserFacingPaymentMethod -> {
                        m.switch(Screen.ParentalWarningRequest(userFacingPaymentMethod.gateway))
                    }
                    is UserFacingPaymentGroup -> {
                        m.switch(Screen.SelectSubGateway(userFacingPaymentMethod))
                    }
                }
            }
        },
    ) {
        Div({
            classes("icon")
        }) {
            Img(
                src = "${window.location.origin}/${userFacingPaymentMethod.imageUrl}",
                // alt = i18nContext.get("gateways.${gateway.name.lowercase()}.name")
            )
        }

        Div({
            classes("method-details")
        }) {
            Div({
                classes("method-title")
            }) {
                Text(i18nContext.get(userFacingPaymentMethod.title))

                if (userFacingPaymentMethod is UserFacingPaymentMethod) {
                    if (userFacingPaymentMethod.country == PaymentMethodCountry.BRAZIL) {
                        Span({
                            classes("only-available-notice")
                        }) {
                            Text("(")
                            TranslatedText(i18nContext, I18nKeysData.OnlyAvailableInBrazil)
                            Text(") ")
                        }
                    }
                }
            }

            Div {
                B {
                    TranslatedText(i18nContext, userFacingPaymentMethod.estimatedTime)
                }
            }

            if (userFacingPaymentMethod is UserFacingPaymentMethod) {
                Div {
                    P {
                        TranslatedText(i18nContext, userFacingPaymentMethod.description)
                    }
                }

                Div {
                    I {
                        val stringBuilder = StringBuilder()
                        var isControl = false

                        for (ch in i18nContext.language.textBundle.strings[I18nKeys.PaymentProcessedViaGateway.key]!!) {
                            if (ch == '{') {
                                Text(stringBuilder.toString())
                                stringBuilder.clear()
                                isControl = true
                                continue
                            }

                            if (isControl && ch == '}') {
                                if (stringBuilder.toString() == "gateway") {
                                    Img(
                                        src = userFacingPaymentMethod.gateway.imageUrl,
                                        attrs = {
                                            style {
                                                height(1.em)
                                                position(Position.Relative)
                                                top(0.2.em)
                                            }
                                        }
                                    )
                                }

                                stringBuilder.clear()
                                isControl = false
                                continue
                            }

                            stringBuilder.append(ch)
                        }

                        if (stringBuilder.isNotEmpty()) {
                            Text(stringBuilder.toString())
                        }
                    }
                }
            }
        }
    }
}