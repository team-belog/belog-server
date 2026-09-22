package org.com.belog.prelog.controller.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.prelog.service.result.PreLogMainResult
import java.time.LocalDate

@Schema(description = "Pre-log 메인 화면 조회 결과")
data class PreLogMainResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val meetingName: String,
    @field:Schema(description = "그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "그룹명", example = "피놀리와 기니휘기")
    val groupName: String,
    @field:Schema(description = "일정 확정 상태", example = "CONFIRMED")
    val meetingStatus: MeetingStatus,
    @field:Schema(description = "시작일. 일정 미확정 시 null", example = "2026-08-26", nullable = true)
    val startDate: LocalDate?,
    @field:Schema(description = "종료일. 일정 미확정 시 null", example = "2026-08-28", nullable = true)
    val endDate: LocalDate?,
    @field:Schema(description = "장소. 미등록 시 null", example = "광주광역시 000 000", nullable = true)
    val location: String?,
    @field:Schema(description = "만남 종료 여부", example = "false")
    @get:JsonProperty("isEnded")
    val isEnded: Boolean,
    @field:Schema(description = "만남 정보 수정 권한", example = "true")
    val canEditMeeting: Boolean,
) {
    companion object {
        fun from(result: PreLogMainResult): PreLogMainResponse =
            PreLogMainResponse(
                meetingId = result.meetingId,
                meetingName = result.meetingName,
                groupId = result.groupId,
                groupName = result.groupName,
                meetingStatus = result.meetingStatus,
                startDate = result.startDate,
                endDate = result.endDate,
                location = result.location,
                isEnded = result.isEnded,
                canEditMeeting = result.canEditMeeting,
            )
    }
}
