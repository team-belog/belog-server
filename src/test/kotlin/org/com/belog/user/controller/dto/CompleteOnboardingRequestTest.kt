package org.com.belog.user.controller.dto

import jakarta.validation.Validation
import org.com.belog.user.domain.Bank
import kotlin.test.Test
import kotlin.test.assertTrue

class CompleteOnboardingRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `프로필 이미지 object key가 공백이면 검증에 실패한다`() {
        val request = validRequest().copy(profileImageObjectKey = " ")

        assertInvalidField(request, "profileImageObjectKey")
    }

    @Test
    fun `프로필 이미지 object key가 최대 길이를 초과하면 검증에 실패한다`() {
        val request = validRequest().copy(profileImageObjectKey = "a".repeat(1025))

        assertInvalidField(request, "profileImageObjectKey")
    }

    @Test
    fun `닉네임이 8자를 초과하면 검증에 실패한다`() {
        val request = validRequest().copy(nickname = "123456789")

        assertInvalidField(request, "nickname")
    }

    @Test
    fun `닉네임 앞뒤 공백을 제거한 길이가 유효하면 검증에 성공한다`() {
        val request = validRequest().copy(nickname = " 빌로그")

        assertTrue(validator.validate(request).none { violation -> violation.propertyPath.toString() == "nickname" })
    }

    @Test
    fun `사용자 이름이 공백뿐이면 검증에 실패한다`() {
        val request = validRequest().copy(name = " ")

        assertInvalidField(request, "name")
    }

    @Test
    fun `사용자 이름이 최대 길이를 초과하면 검증에 실패한다`() {
        val request = validRequest().copy(name = "가".repeat(51))

        assertInvalidField(request, "name")
    }

    @Test
    fun `은행 코드가 없으면 검증에 실패한다`() {
        val request = validRequest().copy(bankCode = null)

        assertInvalidField(request, "bankCode")
    }

    @Test
    fun `계좌번호에 숫자가 아닌 문자가 있으면 검증에 실패한다`() {
        val request = validRequest().copy(accountNumber = "110-123-456789")

        assertInvalidField(request, "accountNumber")
    }

    @Test
    fun `계좌번호가 허용된 길이보다 짧으면 검증에 실패한다`() {
        val request = validRequest().copy(accountNumber = "1234567")

        assertInvalidField(request, "accountNumber")
    }

    @Test
    fun `예금주명이 공백뿐이면 검증에 실패한다`() {
        val request = validRequest().copy(accountHolderName = " ")

        assertInvalidField(request, "accountHolderName")
    }

    @Test
    fun `예금주명이 최대 길이를 초과하면 검증에 실패한다`() {
        val request = validRequest().copy(accountHolderName = "가".repeat(51))

        assertInvalidField(request, "accountHolderName")
    }

    private fun assertInvalidField(
        request: CompleteOnboardingRequest,
        field: String,
    ) {
        val invalidFields = validator.validate(request).map { violation -> violation.propertyPath.toString() }

        assertTrue(field in invalidFields)
    }

    private fun validRequest(): CompleteOnboardingRequest =
        CompleteOnboardingRequest(
            profileImageObjectKey = "users/1/profile/image.webp",
            nickname = "빌로그",
            name = "홍길동",
            bankCode = Bank.SHINHAN,
            accountNumber = "110123456789",
            accountHolderName = "홍길동",
        )
}
