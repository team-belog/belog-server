package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.result.CandidateDateRangeResult
import org.com.belog.meeting.service.result.MeetingDatePollResult
import org.com.belog.meeting.service.result.MyDatePollResponseResult
import java.time.Instant
import java.time.LocalDate

@Schema(description = "후보 일정과 내 응답 상태")
data class MeetingDatePollResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "만남 상태", example = "SCHEDULING")
    val status: MeetingStatus,
    @field:Schema(description = "후보 일정 목록")
    val candidateDateRanges: List<CandidateDateRangeResponse>,
    @field:Schema(description = "내 응답 상태")
    val myResponse: MyDatePollResponse,
) {
    companion object {
        fun from(result: MeetingDatePollResult): MeetingDatePollResponse =
            MeetingDatePollResponse(
                meetingId = result.meetingId,
                status = result.status,
                candidateDateRanges = result.candidateDateRanges.map(CandidateDateRangeResponse::from),
                myResponse = MyDatePollResponse.from(result.myResponse),
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

@Schema(description = "내 후보 일정 응답")
data class MyDatePollResponse(
    @field:Schema(description = "응답 완료 여부", example = "true")
    val responded: Boolean,
    @field:Schema(description = "응답 완료 시각. 미응답이거나 생성자의 기본 응답이면 null입니다.", nullable = true)
    val respondedAt: Instant?,
    @field:Schema(description = "선택한 후보 일정 ID 목록")
    val selectedCandidateDateRangeIds: List<Long>,
) {
    companion object {
        fun from(result: MyDatePollResponseResult): MyDatePollResponse =
            MyDatePollResponse(
                responded = result.responded,
                respondedAt = result.respondedAt,
                selectedCandidateDateRangeIds = result.selectedCandidateDateRangeIds,
            )
    }
}
