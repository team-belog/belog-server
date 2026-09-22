package org.com.belog.prelog.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class PreLogSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    PLAN_CREATED(HttpStatus.CREATED, "PRE_LOG-S001", "계획이 생성되었습니다."),
    PLAN_LIST_RETRIEVED(HttpStatus.OK, "PRE_LOG-S002", "계획 목록을 조회했습니다."),
}
