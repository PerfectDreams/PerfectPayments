package net.perfectdreams.perfectpayments.common.payments

import net.perfectdreams.i18nhelper.core.keydata.StringI18nData
import net.perfectdreams.perfectpayments.i18n.I18nKeysData

sealed class UserFacingPaymentMethodSelection {
    companion object {
        val selections = listOf(
            UserFacingPaymentMethod.Pix,
            UserFacingPaymentGroup.CreditCard,
            UserFacingPaymentGroup.DebitCard,
            UserFacingPaymentMethod.BrazilBankTicket,
            UserFacingPaymentMethod.PicPay,
            UserFacingPaymentMethod.Sandbox
        )
    }

    abstract val title: StringI18nData
    abstract val estimatedTime: StringI18nData
    abstract val imageUrl: String
}

sealed class UserFacingPaymentMethod(
    override val title: StringI18nData,
    val description: StringI18nData,
    override val estimatedTime: StringI18nData,
    override val imageUrl: String,
    val gateway: PaymentGateway,
    val country: PaymentMethodCountry
) : UserFacingPaymentMethodSelection() {
    object Pix : UserFacingPaymentMethod(
        I18nKeysData.Methods.Pix.Title,
        I18nKeysData.Methods.Pix.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/pix.svg",
        PaymentGateway.PAGSEGURO,
        PaymentMethodCountry.BRAZIL
    )
    object BrazilBankTicket : UserFacingPaymentMethod(
        I18nKeysData.Methods.BrazilBankTicket.Title,
        I18nKeysData.Methods.BrazilBankTicket.Description,
        I18nKeysData.PaymentWillBeProcessedWithinThreeBusinessDays,
        "/assets/img/methods/boleto.svg",
        PaymentGateway.PAGSEGURO,
        PaymentMethodCountry.BRAZIL
    )
    object CreditCardPagSeguro : UserFacingPaymentMethod(
        I18nKeysData.Methods.CreditCardPagSeguro.Title,
        I18nKeysData.Methods.CreditCardPagSeguro.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/credit-card.svg",
        PaymentGateway.PAGSEGURO,
        PaymentMethodCountry.GLOBAL
    )
    object CreditCardPayPal : UserFacingPaymentMethod(
        I18nKeysData.Methods.CreditCardPayPal.Title,
        I18nKeysData.Methods.CreditCardPayPal.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/credit-card-paypal.svg",
        PaymentGateway.PAYPAL,
        PaymentMethodCountry.GLOBAL
    )
    object DebitCardPayPal : UserFacingPaymentMethod(
        I18nKeysData.Methods.DebitCard.Title,
        I18nKeysData.Methods.DebitCard.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/debit-card.svg",
        PaymentGateway.PAYPAL,
        PaymentMethodCountry.GLOBAL
    )
    object DebitCardCaixa : UserFacingPaymentMethod(
        I18nKeysData.Methods.DebitCardCaixa.Title,
        I18nKeysData.Methods.DebitCardCaixa.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/debit-card-caixa.svg",
        PaymentGateway.PAGSEGURO,
        PaymentMethodCountry.BRAZIL
    )
    object PicPay : UserFacingPaymentMethod(
        I18nKeysData.Methods.Picpay.Title,
        I18nKeysData.Methods.Picpay.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/gateways/picpay.svg",
        PaymentGateway.PICPAY,
        PaymentMethodCountry.BRAZIL
    )
    object Sandbox : UserFacingPaymentMethod(
        I18nKeysData.Methods.Sandbox.Title,
        I18nKeysData.Methods.Sandbox.Description,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/sandbox.svg",
        PaymentGateway.SANDBOX,
        PaymentMethodCountry.GLOBAL
    )
}

sealed class UserFacingPaymentGroup(
    override val title: StringI18nData,
    override val estimatedTime: StringI18nData,
    override val imageUrl: String,
    val methods: List<UserFacingPaymentMethod>
) : UserFacingPaymentMethodSelection() {
    object CreditCard : UserFacingPaymentGroup(
        I18nKeysData.Methods.CreditCard.Title,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/credit-card.svg",
        listOf(
            UserFacingPaymentMethod.CreditCardPagSeguro,
            UserFacingPaymentMethod.CreditCardPayPal
        )
    )

    object DebitCard : UserFacingPaymentGroup(
        I18nKeysData.Methods.DebitCard.Title,
        I18nKeysData.PaymentWillBeProcessedWithinOneHour,
        "/assets/img/methods/debit-card.svg",
        listOf(
            UserFacingPaymentMethod.DebitCardPayPal,
            UserFacingPaymentMethod.DebitCardCaixa
        )
    )
}