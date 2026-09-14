package org.com.belog.user.infrastructure

import org.com.belog.user.config.AccountNumberEncryptionProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class AccountNumberAttributeConverterTest {
    private val converter =
        AccountNumberAttributeConverter(
            AccountNumberEncryptionProperties(TEST_ENCRYPTION_KEY),
        )

    @Test
    fun `계좌번호를 암호화하고 다시 복호화한다`() {
        val encryptedAccountNumber = requireNotNull(converter.convertToDatabaseColumn(ACCOUNT_NUMBER))

        assertNotEquals(ACCOUNT_NUMBER, encryptedAccountNumber)
        assertEquals(ACCOUNT_NUMBER, converter.convertToEntityAttribute(encryptedAccountNumber))
    }

    @Test
    fun `같은 계좌번호도 매번 다른 암호문으로 저장한다`() {
        val firstEncryptedAccountNumber = converter.convertToDatabaseColumn(ACCOUNT_NUMBER)
        val secondEncryptedAccountNumber = converter.convertToDatabaseColumn(ACCOUNT_NUMBER)

        assertNotEquals(firstEncryptedAccountNumber, secondEncryptedAccountNumber)
    }

    @Test
    fun `변조된 암호문은 복호화할 수 없다`() {
        val encryptedAccountNumber = requireNotNull(converter.convertToDatabaseColumn(ACCOUNT_NUMBER))
        val tamperedEncryptedAccountNumber = encryptedAccountNumber.dropLast(1) + "A"

        assertFailsWith<IllegalStateException> {
            converter.convertToEntityAttribute(tamperedEncryptedAccountNumber)
        }
    }

    companion object {
        private const val ACCOUNT_NUMBER = "110123456789"
        private const val TEST_ENCRYPTION_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
    }
}
