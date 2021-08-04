package net.perfectdreams.perfectpayments.common.payments

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentGateway(val imageUrl: String, val methods: List<PaymentMethod>) {
    UNKNOWN(
        "/assets/img/gateways/unknown.png",
        listOf()
    ),
    PICPAY(
        "/assets/img/gateways/picpay.svg",
        listOf(
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.PICPAY_BALANCE
        )
    ),
    PAGSEGURO(
        "/assets/img/gateways/pagseguro.svg",
        listOf(
            PaymentMethod.BRAZIL_BANK_TICKET,
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.PIX
        )
    ),
    STRIPE(
        "/assets/img/gateways/stripe.svg",
        listOf(
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.GOOGLE_PAY,
            PaymentMethod.APPLE_PAY
        )
    ),
    PAYPAL(
        "/assets/img/gateways/paypal.svg",
        listOf(
            PaymentMethod.CREDIT_CARD,
            PaymentMethod.PAYPAL_BALANCE
        )
    ),
    SANDBOX(
        "/assets/img/gateways/paypal.svg",
        listOf(
            PaymentMethod.SANDBOX
        )
    ),
}