package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.service.result.PastMeetingListResult
import org.com.belog.meeting.service.result.PastMeetingResult
import java.time.LocalDate

@Schema(description = "지난 만남 목록 조회 결과")
data class PastMeetingListResponse(
    @field:Schema(description = "지난 만남 목록")
    val items: List<PastMeetingResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", example = "7", nullable = true)
    val nextCursor: Long?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: PastMeetingListResult): PastMeetingListResponse =
            PastMeetingListResponse(
                items = result.items.map(PastMeetingResponse::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "지난 만남")
data class PastMeetingResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "2025 연말 파티")
    val name: String,
    @field:Schema(description = "시작일", example = "2025-12-30")
    val startDate: LocalDate,
    @field:Schema(description = "종료일", example = "2025-12-30")
    val endDate: LocalDate,
) {
    companion object {
        fun from(result: PastMeetingResult): PastMeetingResponse =
            PastMeetingResponse(
                meetingId = result.meetingId,
                name = result.name,
                startDate = result.startDate,
                endDate = result.endDate,
            )
    }
}
