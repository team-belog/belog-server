package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.result.CandidateDateRangeResult
import org.com.belog.meeting.service.result.MeetingDatePollResult
import java.time.LocalDate

@Schema(description = "후보 일정")
data class MeetingDatePollResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "만남 상태", example = "SCHEDULING")
    val status: MeetingStatus,
    @field:Schema(description = "후보 일정 목록")
    val candidateDateRanges: List<CandidateDateRangeResponse>,
) {
    companion object {
        fun from(result: MeetingDatePollResult): MeetingDatePollResponse =
            MeetingDatePollResponse(
                meetingId = result.meetingId,
                status = result.status,
                candidateDateRanges = result.candidateDateRanges.map(CandidateDateRangeResponse::from),
            )
    }
}

@Schema(description = "후보 일정 범위")
data class CandidateDateRangeResponse(
    @field:Schema(description = "후보 일정 ID", example = "101")
    val id: Long,
    @field:Schema(description = "시작일", example = "2026-10-03")
    val startDate: LocalDate,
    @field:Schema(description = "종료일", example = "2026-10-04")
    val endDate: LocalDate,
) {
    companion object {
        fun from(result: CandidateDateRangeResult): CandidateDateRangeResponse =
            CandidateDateRangeResponse(
                id = result.id,
                startDate = result.startDate,
                endDate = result.endDate,
            )
    }
}
