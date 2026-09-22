package org.com.belog.prelog.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class PreLogErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_PLAN(HttpStatus.BAD_REQUEST, "PRE_LOG-E001", "계획 정보가 올바르지 않습니다."),
    MEETING_ALREADY_ENDED(HttpStatus.CONFLICT, "PRE_LOG-E002", "종료된 만남에는 계획을 추가할 수 없습니다."),
}
