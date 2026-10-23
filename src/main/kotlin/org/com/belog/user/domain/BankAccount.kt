package org.com.belog.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter

@Embeddable
class BankAccount protected constructor(
    @Enumerated(EnumType.STRING)
    @Column(name = "bank", length = 30)
    val bank: Bank,
    @Convert(converter = AccountNumberAttributeConverter::class)
    @Column(name = "encrypted_account_number", length = AccountNumberAttributeConverter.ENCRYPTED_COLUMN_MAX_LENGTH)
    val accountNumber: String,
) {
    companion object {
        const val ACCOUNT_NUMBER_MIN_LENGTH = 8
        const val ACCOUNT_NUMBER_MAX_LENGTH = 20

        fun create(
            bank: Bank,
            accountNumber: String,
        ): BankAccount {
            require(accountNumber.all(Char::isDigit)) { "계좌번호는 숫자만 포함할 수 있습니다." }
            require(accountNumber.length in ACCOUNT_NUMBER_MIN_LENGTH..ACCOUNT_NUMBER_MAX_LENGTH) {
                "계좌번호는 ${ACCOUNT_NUMBER_MIN_LENGTH}자 이상 ${ACCOUNT_NUMBER_MAX_LENGTH}자 이하여야 합니다."
            }

            return BankAccount(
                bank = bank,
                accountNumber = accountNumber,
            )
        }
    }
}
