package org.com.belog.user.config

import kotlin.test.Test
import kotlin.test.assertFailsWith

class AccountNumberEncryptionPropertiesTest {
    @Test
    fun `암호화 키가 Base64 형식이 아니면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            AccountNumberEncryptionProperties("not-base64")
        }
    }

    @Test
    fun `암호화 키가 32바이트가 아니면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            AccountNumberEncryptionProperties("c2hvcnQ=")
        }
    }
}
