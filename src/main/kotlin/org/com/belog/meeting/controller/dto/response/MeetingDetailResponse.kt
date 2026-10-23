package org.com.belog.meeting.controller.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.domain.MeetingLogStatus
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.result.MeetingDetailResult
import java.time.LocalDate

@Schema(description = "만남 상세 조회 결과")
data class MeetingDetailResponse(
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "그룹 ID", example = "1")
    val groupId: Long,
    @field:Schema(description = "그룹명", example = "피놀리와 기니휘기")
    val groupName: String,
    @field:Schema(description = "만남명", example = "1박 2일 광주 여행")
    val meetingName: String,
    @field:Schema(description = "일정 등록 방식", example = "FIXED")
    val scheduleType: MeetingScheduleType,
    @field:Schema(description = "만남 상태", example = "CONFIRMED")
    val meetingStatus: MeetingStatus,
    @field:Schema(description = "시작일. 일정 조율 중에는 null", example = "2026-10-03", nullable = true)
    val startDate: LocalDate?,
    @field:Schema(description = "종료일. 일정 조율 중에는 null", example = "2026-10-04", nullable = true)
    val endDate: LocalDate?,
    @field:Schema(description = "장소. 미등록 시 null", example = "광주광역시 000 000", nullable = true)
    val location: String?,
    @field:Schema(description = "만남 종료 여부", example = "false")
    @get:JsonProperty("isEnded")
    val isEnded: Boolean,
    @field:Schema(description = "만남 정보 수정 권한", example = "true")
    val canEditMeeting: Boolean,
    @field:Schema(
        description =
            "만남 종료 가능 여부. 만남 방장이고, 일정이 확정되어 시작일이 지났으며, " +
                "Pre-log, Bill-log, Post-log가 모두 작성 중인 종료 전 만남이면 true",
        example = "false",
    )
    val canEndMeeting: Boolean,
    @field:Schema(description = "로그별 작성 상태")
    val logStatuses: MeetingLogStatusesResponse,
) {
    companion object {
        fun from(result: MeetingDetailResult): MeetingDetailResponse =
            MeetingDetailResponse(
                meetingId = result.meetingId,
                groupId = result.groupId,
                groupName = result.groupName,
                meetingName = result.meetingName,
                scheduleType = result.scheduleType,
                meetingStatus = result.meetingStatus,
                startDate = result.startDate,
                endDate = result.endDate,
                location = result.location,
                isEnded = result.isEnded,
                canEditMeeting = result.canEditMeeting,
                canEndMeeting = result.canEndMeeting,
                logStatuses =
                    MeetingLogStatusesResponse(
                        preLog = result.preLogStatus,
                        billLog = result.billLogStatus,
                        postLog = result.postLogStatus,
                    ),
            )
    }
}

@Schema(description = "로그별 작성 상태")
data class MeetingLogStatusesResponse(
    @field:Schema(description = "Pre-log 작성 상태", example = "IN_PROGRESS")
    val preLog: MeetingLogStatus,
    @field:Schema(description = "Bill-log 작성 상태", example = "NOT_STARTED")
    val billLog: MeetingLogStatus,
    @field:Schema(description = "Post-log 작성 상태", example = "NOT_STARTED")
    val postLog: MeetingLogStatus,
)
