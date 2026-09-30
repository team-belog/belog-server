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
    PRE_LOG_MAIN_RETRIEVED(HttpStatus.OK, "PRE_LOG-S003", "Pre-log 메인 정보를 조회했습니다."),
    PLAN_LIKED(HttpStatus.OK, "PRE_LOG-S005", "계획에 좋아요를 등록했습니다."),
    PLAN_LIKE_CANCELED(HttpStatus.OK, "PRE_LOG-S006", "계획 좋아요를 취소했습니다."),
}
