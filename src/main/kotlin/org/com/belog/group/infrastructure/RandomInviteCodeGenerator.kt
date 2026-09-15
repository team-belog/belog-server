package org.com.belog.group.infrastructure

import org.com.belog.group.domain.InviteCode
import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class RandomInviteCodeGenerator {
    private val secureRandom = SecureRandom()

    fun generate(): InviteCode {
        val characters =
            CharArray(InviteCode.LENGTH) { index ->
                when (index) {
                    0 -> LETTERS.randomCharacter()
                    1 -> DIGITS.randomCharacter()
                    else -> ALPHANUMERIC_CHARACTERS.randomCharacter()
                }
            }

        characters.shuffle()
        return InviteCode.create(characters.concatToString())
    }

    private fun String.randomCharacter(): Char = this[secureRandom.nextInt(length)]

    private fun CharArray.shuffle() {
        for (index in lastIndex downTo 1) {
            val targetIndex = secureRandom.nextInt(index + 1)
            val current = this[index]
            this[index] = this[targetIndex]
            this[targetIndex] = current
        }
    }

    companion object {
        private const val LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private const val DIGITS = "0123456789"
        private const val ALPHANUMERIC_CHARACTERS = LETTERS + DIGITS
    }
}
