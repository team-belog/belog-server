package org.com.belog.meeting.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class MeetingErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_PARTICIPANT(HttpStatus.BAD_REQUEST, "MEETING-E001", "만남 참여자는 해당 그룹의 멤버여야 합니다."),
    DUPLICATE_PARTICIPANT(HttpStatus.BAD_REQUEST, "MEETING-E002", "만남 참여자가 중복되었습니다."),
    CREATOR_INCLUDED_AS_PARTICIPANT(HttpStatus.BAD_REQUEST, "MEETING-E003", "만남 생성자는 참여자 목록에 포함할 수 없습니다."),
    PARTICIPANT_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "MEETING-E004", "만남 참여 인원이 그룹 최대 인원을 초과했습니다."),
    PAST_MEETING_DATE(HttpStatus.BAD_REQUEST, "MEETING-E005", "과거 날짜로 만남을 생성할 수 없습니다."),
    UNSUPPORTED_SCHEDULE_TYPE(HttpStatus.BAD_REQUEST, "MEETING-E006", "지원하지 않는 일정 등록 방식입니다."),
    INVALID_MEETING_DATE_RANGE(HttpStatus.BAD_REQUEST, "MEETING-E007", "만남 종료일은 시작일보다 빠를 수 없습니다."),
    INVALID_CANDIDATE_DATE_RANGE_COUNT(HttpStatus.BAD_REQUEST, "MEETING-E008","후보 일정 범위는 2개 이상 10개 이하로 등록해야 합니다."),
    DUPLICATE_CANDIDATE_DATE_RANGE(HttpStatus.BAD_REQUEST, "MEETING-E009", "중복된 후보 일정 범위가 포함되어 있습니다."),
}
