package org.com.belog.user.controller.dto.response

import org.com.belog.user.domain.Bank

data class BankResponse(
    val code: String,
    val displayName: String,
) {
    companion object {
        fun from(bank: Bank): BankResponse =
            BankResponse(
                code = bank.name,
                displayName = bank.displayName,
            )
    }
}
