package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.result.CreatedMeeting
import java.time.Instant
import java.time.LocalDate

data class CreateMeetingResponse(
    @field:Schema(description = "만남 ID", example = "1")
    val meetingId: Long,
    @field:Schema(description = "그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "만남명", example = "광주 1박 2일")
    val name: String,
    @field:Schema(description = "만남 장소", example = "서울고속버스터미널", nullable = true)
    val location: String?,
    @field:Schema(description = "일정 등록 방식", example = "FIXED")
    val scheduleType: MeetingScheduleType,
    @field:Schema(description = "만남 상태", example = "CONFIRMED")
    val status: MeetingStatus,
    @field:Schema(description = "일정 시작일", example = "2026-10-03")
    val startDate: LocalDate,
    @field:Schema(description = "일정 종료일", example = "2026-10-04")
    val endDate: LocalDate,
    @field:Schema(description = "날짜 확정 시각", example = "2026-09-21T00:00:00Z")
    val confirmedAt: Instant,
    @field:Schema(description = "생성자를 포함한 참여 인원", example = "3")
    val participantCount: Int,
) {
    companion object {
        fun from(createdMeeting: CreatedMeeting): CreateMeetingResponse =
            CreateMeetingResponse(
                meetingId = createdMeeting.meetingId,
                groupId = createdMeeting.groupId,
                name = createdMeeting.name,
                location = createdMeeting.location,
                scheduleType = createdMeeting.scheduleType,
                status = createdMeeting.status,
                startDate = createdMeeting.startDate,
                endDate = createdMeeting.endDate,
                confirmedAt = createdMeeting.confirmedAt,
                participantCount = createdMeeting.participantCount,
            )
    }
}
