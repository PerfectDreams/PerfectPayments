package net.perfectdreams.perfectpayments.notafiscais

// https://focusnfe.com.br/doc/#gatilhos-webhooks_gatilhos-webhooks
enum class NotaFiscalStatus {
    UNKNOWN,
    CREATED,
    PROCESSING_AUTHORIZATION,
    AUTHORIZED,
    CANCELLED,
    AUTHORIZATION_ERROR
}