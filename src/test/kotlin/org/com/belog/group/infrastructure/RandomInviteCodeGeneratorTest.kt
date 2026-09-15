package org.com.belog.group.infrastructure

import org.com.belog.group.domain.InviteCode
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class RandomInviteCodeGeneratorTest {
    private val generator = RandomInviteCodeGenerator()

    @Test
    fun `영문 대문자와 숫자가 조합된 6자리 초대 코드를 생성한다`() {
        repeat(100) {
            val inviteCode = generator.generate().value

            assertTrue(inviteCode.length == InviteCode.LENGTH)
            assertTrue(inviteCode.all { character -> character.isDigit() || character in 'A'..'Z' })
            assertTrue(inviteCode.any(Char::isDigit))
            assertTrue(inviteCode.any { character -> character in 'A'..'Z' })
        }
    }
}
