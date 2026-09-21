package org.com.belog.meeting.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class MeetingSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    MEETING_CREATED(HttpStatus.CREATED, "MEETING-S001", "만남이 생성되었습니다."),
    DATE_POLL_RETRIEVED(HttpStatus.OK, "MEETING-S002", "후보 일정을 조회했습니다."),
    DATE_POLL_RESPONSE_SUBMITTED(HttpStatus.OK, "MEETING-S003", "가능한 후보 일정 응답을 완료했습니다."),
    DATE_POLL_RESULTS_RETRIEVED(HttpStatus.OK, "MEETING-S004", "후보 일정 조율 현황을 조회했습니다."),
}
