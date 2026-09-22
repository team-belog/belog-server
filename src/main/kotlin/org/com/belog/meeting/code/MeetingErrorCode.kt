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
    INVALID_CANDIDATE_DATE_RANGE_COUNT(HttpStatus.BAD_REQUEST, "MEETING-E008", "후보 일정 범위는 2개 이상 10개 이하로 등록해야 합니다."),
    DUPLICATE_CANDIDATE_DATE_RANGE(HttpStatus.BAD_REQUEST, "MEETING-E009", "중복된 후보 일정 범위가 포함되어 있습니다."),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "MEETING-E010", "만남을 찾을 수 없습니다."),
    NOT_MEETING_PARTICIPANT(HttpStatus.FORBIDDEN, "MEETING-E011", "해당 만남의 참여자가 아닙니다."),
    NOT_DATE_POLL_MEETING(HttpStatus.BAD_REQUEST, "MEETING-E012", "일정 조율 방식의 만남이 아닙니다."),
    CREATOR_CANNOT_RESPOND_DATE_POLL(HttpStatus.FORBIDDEN, "MEETING-E013", "만남 생성자는 후보 일정에 응답할 수 없습니다."),
    DATE_POLL_NOT_SCHEDULING(HttpStatus.CONFLICT, "MEETING-E014", "일정 조율 중인 만남에만 응답할 수 있습니다."),
    DATE_POLL_ALREADY_RESPONDED(HttpStatus.CONFLICT, "MEETING-E015", "후보 일정 응답을 이미 완료했습니다."),
    DUPLICATE_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "MEETING-E016", "중복된 후보 일정이 선택되었습니다."),
    INVALID_AVAILABLE_DATE(HttpStatus.BAD_REQUEST, "MEETING-E017", "해당 만남의 후보 일정이 아닙니다."),
    NOT_MEETING_CREATOR(HttpStatus.FORBIDDEN, "MEETING-E018", "만남 생성자만 일정을 관리할 수 있습니다."),
    MEETING_ALREADY_ENDED(HttpStatus.CONFLICT, "MEETING-E019", "종료된 만남의 일정은 변경할 수 없습니다."),
    MEETING_DATE_NOT_SCHEDULING(HttpStatus.CONFLICT, "MEETING-E020", "일정 조율 중인 만남만 확정할 수 있습니다."),
    MEETING_DATE_NOT_CONFIRMED(HttpStatus.CONFLICT, "MEETING-E021", "확정된 만남의 일정만 변경할 수 있습니다."),
    PAST_MEETING_DATE_SELECTION(HttpStatus.BAD_REQUEST, "MEETING-E022", "과거 날짜를 만남 일정으로 지정할 수 없습니다."),
}
