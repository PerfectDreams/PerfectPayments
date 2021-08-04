package net.perfectdreams.perfectpayments.backend.utils

import net.perfectdreams.perfectpayments.backend.config.PagSeguroConfig
import net.perfectdreams.perfectpayments.backend.config.PayPalConfig
import net.perfectdreams.perfectpayments.backend.config.PicPayConfig
import net.perfectdreams.perfectpayments.backend.config.StripeConfig
import net.perfectdreams.perfectpayments.common.payments.PaymentGateway

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