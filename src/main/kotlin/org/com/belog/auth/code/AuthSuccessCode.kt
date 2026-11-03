package org.com.belog.auth.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class AuthSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    GOOGLE_LOGIN_SUCCESS(HttpStatus.OK, "AUTH-S001", "Google 로그인에 성공했습니다."),
    TOKEN_REISSUE_SUCCESS(HttpStatus.OK, "AUTH-S002", "토큰 재발급에 성공했습니다."),
}
