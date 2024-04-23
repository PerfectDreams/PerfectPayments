package net.perfectdreams.perfectpayments.backend.processors.creators

sealed class CreatedPaymentInfo(
    val id: String
)

sealed class CreatedPaymentInfoWithUrl(
    id: String,
    val url: String
) : CreatedPaymentInfo(id)

class CreatedPayPalPaymentInfo(
    id: String,
    url: String
) : CreatedPaymentInfoWithUrl(id, url)

class CreatedPagSeguroPaymentInfo(
    id: String,
    url: String
) : CreatedPaymentInfoWithUrl(id, url)

class CreatedPicPayPaymentInfo(
    id: String,
    url: String
) : CreatedPaymentInfoWithUrl(id, url)

class CreatedSandboxPaymentInfo(
    id: String
) : CreatedPaymentInfo(id)

class CreatedMercadoPagoPaymentInfo(
    id: String,
    url: String
) : CreatedPaymentInfoWithUrl(id, url)