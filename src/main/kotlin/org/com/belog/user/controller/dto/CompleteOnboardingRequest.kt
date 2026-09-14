package org.com.belog.user.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.com.belog.user.controller.validation.ValidNickname
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.User

data class CompleteOnboardingRequest(
    @field:NotBlank(message = "프로필 이미지 object key는 비어 있을 수 없습니다.")
    @field:Size(
        max = ProfileImageObjectKey.MAX_LENGTH,
        message = "프로필 이미지 object key는 ${ProfileImageObjectKey.MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    @field:Pattern(regexp = "^\\S+$", message = "프로필 이미지 object key에는 공백을 포함할 수 없습니다.")
    val profileImageObjectKey: String,
    @field:Schema(
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = User.NICKNAME_MIN_LENGTH,
        maxLength = User.NICKNAME_MAX_LENGTH,
    )
    @field:ValidNickname
    val nickname: String,
    @field:NotNull(message = "은행 코드는 필수입니다.")
    val bankCode: Bank?,
    @field:NotBlank(message = "계좌번호는 비어 있을 수 없습니다.")
    @field:Size(
        min = BankAccount.ACCOUNT_NUMBER_MIN_LENGTH,
        max = BankAccount.ACCOUNT_NUMBER_MAX_LENGTH,
        message =
            "계좌번호는 ${BankAccount.ACCOUNT_NUMBER_MIN_LENGTH}자 이상 " +
                "${BankAccount.ACCOUNT_NUMBER_MAX_LENGTH}자 이하여야 합니다.",
    )
    @field:Pattern(regexp = "^\\d+$", message = "계좌번호는 숫자만 포함할 수 있습니다.")
    val accountNumber: String,
    @field:NotBlank(message = "예금주명은 비어 있을 수 없습니다.")
    @field:Size(
        max = BankAccount.ACCOUNT_HOLDER_NAME_MAX_LENGTH,
        message = "예금주명은 ${BankAccount.ACCOUNT_HOLDER_NAME_MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    val accountHolderName: String,
)
