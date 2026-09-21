package org.com.belog.meeting.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class MeetingSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    MEETING_CREATED(HttpStatus.CREATED, "MEETING-S001", "만남이 생성되었습니다."),
}
