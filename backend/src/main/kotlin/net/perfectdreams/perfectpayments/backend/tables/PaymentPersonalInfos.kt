package net.perfectdreams.perfectpayments.backend.tables

import org.jetbrains.exposed.dao.id.LongIdTable

object PaymentPersonalInfos : LongIdTable() {
    val payment = reference("payment", Payments)
    val socialNumber = text("social_number") // Can be CPF or CNPJ
    val name = text("name")
    val email = text("email")
}