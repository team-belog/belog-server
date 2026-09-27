package org.com.belog.user.controller.dto

import org.com.belog.user.domain.BankAccount

data class BankAccountResponse(
    val bankCode: String,
    val bankName: String,
    val accountNumber: String,
    val accountHolderName: String,
) {
    companion object {
        fun from(bankAccount: BankAccount): BankAccountResponse =
            BankAccountResponse(
                bankCode = bankAccount.bank.name,
                bankName = bankAccount.bank.displayName,
                accountNumber = bankAccount.accountNumber,
                accountHolderName = bankAccount.accountHolderName,
            )
    }
}
