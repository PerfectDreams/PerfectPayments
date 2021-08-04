package net.perfectdreams.perfectpayments.backend.dao

import net.perfectdreams.perfectpayments.backend.tables.NotaFiscais
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class NotaFiscal(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<NotaFiscal>(NotaFiscais)

    var payment by Payment referencedOn NotaFiscais.payment
    var personalInfo by PaymentPersonalInfo optionalReferencedOn NotaFiscais.personalInfo
    var status by NotaFiscais.status
}