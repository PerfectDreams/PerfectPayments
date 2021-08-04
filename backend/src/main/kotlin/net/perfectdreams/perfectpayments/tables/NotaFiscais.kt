package net.perfectdreams.perfectpayments.tables

import net.perfectdreams.perfectpayments.notafiscais.NotaFiscalStatus
import net.perfectdreams.perfectpayments.payments.PaymentStatus
import org.jetbrains.exposed.dao.id.LongIdTable

object NotaFiscais : LongIdTable() {
    val payment = reference("payment", Payments)
    val personalInfo = optReference("personal_info", PaymentPersonalInfos)
    val status = enumeration("status", NotaFiscalStatus::class)
}