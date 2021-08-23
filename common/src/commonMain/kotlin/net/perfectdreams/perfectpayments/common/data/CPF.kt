package net.perfectdreams.perfectpayments.common.data

import kotlinx.serialization.Serializable

@Serializable
class CPF private constructor(
    val cleanDocument: String
) {
    companion object {
        operator fun invoke(input: String): CPF {
            val cleanDocument = input.replace(".", "").replace("-", "").replace(" ", "")
            cleanDocument.toLongOrNull() ?: throw IllegalArgumentException("Input can't be converted into a Long!")
            return CPF(cleanDocument)
        }
    }

    init {
        require(cleanDocument.length == 11) { "Input length is incorrect! CPFs have 11 characters in length!" }

        var calculation = 0
        for (index in 0 until 9) {
            calculation += (cleanDocument[index].digitToInt() * (10 - index))
        }

        var lastStepOfTheVerification = (calculation * 10) % 11
        if (lastStepOfTheVerification == 10)
            lastStepOfTheVerification = 0

        val isValid = cleanDocument[9].digitToInt() == lastStepOfTheVerification
        require(isValid) { "Input didn't pass CPF digit verification test!" }
    }
}