package org.com.belog.home.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class HomeSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    HOME_CALENDAR_RETRIEVED(HttpStatus.OK, "HOME-S001", "홈 달력을 조회했습니다."),
}
