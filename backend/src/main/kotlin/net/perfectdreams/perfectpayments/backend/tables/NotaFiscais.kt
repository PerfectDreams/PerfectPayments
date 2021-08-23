package net.perfectdreams.perfectpayments.backend.tables

import net.perfectdreams.perfectpayments.backend.notafiscais.NotaFiscalStatus
import org.jetbrains.exposed.dao.id.LongIdTable

object NotaFiscais : LongIdTable() {
    val payment = reference("payment", Payments)
    val personalInfo = optReference("personal_info", PaymentPersonalInfos)
    val status = enumeration("status", NotaFiscalStatus::class)
    val url = text("url").nullable()
}