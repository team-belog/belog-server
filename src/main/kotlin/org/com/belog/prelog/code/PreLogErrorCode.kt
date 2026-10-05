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
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PRE_LOG-E003", "계획을 찾을 수 없습니다."),
    PLAN_UPDATE_FORBIDDEN(HttpStatus.FORBIDDEN, "PRE_LOG-E004", "계획 작성자만 수정할 수 있습니다."),
    PLAN_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "PRE_LOG-E005", "계획을 삭제할 권한이 없습니다."),
    PLAN_UPDATE_CONFLICT(HttpStatus.CONFLICT, "PRE_LOG-E006", "계획이 다른 요청에서 수정되었습니다. 다시 시도해 주세요."),
}
