package org.com.belog.home.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class HomeErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "HOME-E001", "홈 만남 목록 커서가 올바르지 않습니다."),
}
