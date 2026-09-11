package org.com.belog.auth.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_GOOGLE_ID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-E001", "유효하지 않은 Google 인증 정보입니다."),
    INVALID_GOOGLE_AUTHORIZATION_CODE(HttpStatus.UNAUTHORIZED, "AUTH-E002", "유효하지 않은 Google 인가 코드입니다."),
    GOOGLE_AUTH_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH-E003", "Google 인증 서버와 통신할 수 없습니다."),
    INVALID_GOOGLE_REDIRECT_URI(HttpStatus.BAD_REQUEST, "AUTH-E004", "허용되지 않은 Google 리디렉션 URI입니다."),
}
