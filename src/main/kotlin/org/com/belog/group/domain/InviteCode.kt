package org.com.belog.group.domain

@JvmInline
value class InviteCode private constructor(
    val value: String,
) {
    companion object {
        const val LENGTH = 6
        private val PATTERN = Regex("^(?=.*[A-Z])(?=.*[0-9])[A-Z0-9]{$LENGTH}$")

        fun create(value: String): InviteCode {
            require(PATTERN.matches(value)) {
                "초대 코드는 영문 대문자와 숫자를 조합한 ${LENGTH}자리여야 합니다."
            }

            return InviteCode(value)
        }
    }
}
