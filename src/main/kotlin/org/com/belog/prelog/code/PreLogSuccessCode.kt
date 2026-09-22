package org.com.belog.prelog.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class PreLogSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    PLAN_CREATED(HttpStatus.CREATED, "PRE_LOG-S001", "계획이 생성되었습니다."),
}
