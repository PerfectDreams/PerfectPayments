package net.perfectdreams.perfectpayments.utils

import net.perfectdreams.perfectpayments.config.PagSeguroConfig
import net.perfectdreams.perfectpayments.config.PayPalConfig
import net.perfectdreams.perfectpayments.config.PicPayConfig
import net.perfectdreams.perfectpayments.config.StripeConfig
import net.perfectdreams.perfectpayments.payments.PaymentGateway

class GatewayConfigs(val map: Map<PaymentGateway, Any>) {
    val picPay: PicPayConfig
        get() = map[PaymentGateway.PICPAY] as PicPayConfig
    val pagSeguro: PagSeguroConfig
        get() = map[PaymentGateway.PAGSEGURO] as PagSeguroConfig
    val stripe: StripeConfig
        get() = map[PaymentGateway.STRIPE] as StripeConfig
    val payPal: PayPalConfig
        get() = map[PaymentGateway.PAYPAL] as PayPalConfig
}