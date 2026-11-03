package org.com.belog.group.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InviteCodeTest {
    @Test
    fun `영문 대문자와 숫자가 조합된 6자리 초대 코드를 생성한다`() {
        val inviteCode = InviteCode.create("AB12CD")

        assertEquals("AB12CD", inviteCode.value)
    }

    @Test
    fun `영문이나 숫자 중 하나만 포함된 초대 코드는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { InviteCode.create("ABCDEF") }
        assertFailsWith<IllegalArgumentException> { InviteCode.create("123456") }
    }

    @Test
    fun `6자리가 아니거나 소문자가 포함된 초대 코드는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { InviteCode.create("AB12C") }
        assertFailsWith<IllegalArgumentException> { InviteCode.create("Ab12CD") }
    }
}
