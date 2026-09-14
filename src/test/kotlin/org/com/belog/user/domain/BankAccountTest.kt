package org.com.belog.user.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith

class BankAccountTest {
    @Test
    fun `계좌번호가 비어 있으면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            BankAccount.create(
                bank = Bank.SHINHAN,
                accountNumber = "",
                accountHolderName = "홍길동",
            )
        }
    }

    @Test
    fun `계좌번호에 숫자가 아닌 문자가 있으면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            BankAccount.create(
                bank = Bank.SHINHAN,
                accountNumber = "110-ABC-456789",
                accountHolderName = "홍길동",
            )
        }
    }

    @Test
    fun `예금주명이 비어 있으면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            BankAccount.create(
                bank = Bank.SHINHAN,
                accountNumber = "110123456789",
                accountHolderName = " ",
            )
        }
    }
}
