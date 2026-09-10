package org.com.belog.auth.response.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_GOOGLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-E001", "유효하지 않은 Google 인증 정보입니다."),
}
