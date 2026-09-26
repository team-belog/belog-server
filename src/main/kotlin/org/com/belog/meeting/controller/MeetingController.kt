package org.com.belog.meeting.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.code.MeetingSuccessCode
import org.com.belog.meeting.controller.dto.request.CreateMeetingRequest
import org.com.belog.meeting.controller.dto.request.MeetingDateRangeRequest
import org.com.belog.meeting.controller.dto.response.CreateMeetingResponse
import org.com.belog.meeting.controller.dto.response.MeetingDetailResponse
import org.com.belog.meeting.controller.dto.response.PastMeetingListResponse
import org.com.belog.meeting.controller.swagger.MeetingSwagger
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.service.MeetingDetailService
import org.com.belog.meeting.service.MeetingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class MeetingController(
    private val meetingService: MeetingService,
    private val meetingDetailService: MeetingDetailService,
) : MeetingSwagger {
    @GetMapping("/api/v1/groups/{groupId}/meetings/past")
    override fun getPastMeetings(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
        @RequestParam(required = false) @Positive cursor: Long?,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<PastMeetingListResponse>> {
        val result =
            meetingService.getPastMeetings(
                groupId = groupId,
                userId = userId,
                cursor = cursor,
                size = size,
            )

        return ResponseEntity
            .status(MeetingSuccessCode.PAST_MEETINGS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.PAST_MEETINGS_RETRIEVED,
                    PastMeetingListResponse.from(result),
                ),
            )
    }

    @PostMapping("/api/v1/groups/{groupId}/meetings")
    override fun createMeeting(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateMeetingRequest,
    ): ResponseEntity<CommonResponse<CreateMeetingResponse>> {
        val createdMeeting =
            when (request.schedule.type) {
                MeetingScheduleType.FIXED -> {
                    val dateRange = request.schedule.dateRanges.single()
                    meetingService.createFixedMeeting(
                        groupId = groupId,
                        creatorUserId = userId,
                        name = request.name,
                        location = request.location,
                        participantMemberIds = request.participantMemberIds,
                        startDate = dateRange.startDate,
                        endDate = dateRange.endDate,
                    )
                }

                MeetingScheduleType.POLL ->
                    meetingService.createPollMeeting(
                        groupId = groupId,
                        creatorUserId = userId,
                        name = request.name,
                        location = request.location,
                        participantMemberIds = request.participantMemberIds,
                        candidateDateRanges = request.schedule.dateRanges.map { it.toDomain() },
                    )
            }

        return ResponseEntity
            .status(MeetingSuccessCode.MEETING_CREATED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.MEETING_CREATED,
                    CreateMeetingResponse.from(createdMeeting),
                ),
            )
    }

    @GetMapping("/api/v1/meetings/{meetingId}")
    override fun getMeetingDetail(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<CommonResponse<MeetingDetailResponse>> {
        val result = meetingDetailService.getMeetingDetail(meetingId = meetingId, userId = userId)

        return ResponseEntity
            .status(MeetingSuccessCode.MEETING_DETAIL_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.MEETING_DETAIL_RETRIEVED,
                    MeetingDetailResponse.from(result),
                ),
            )
    }

    private fun MeetingDateRangeRequest.toDomain(): MeetingDateRange =
        try {
            MeetingDateRange(
                startDate = startDate,
                endDate = endDate,
            )
        } catch (_: IllegalArgumentException) {
            throw BusinessException(MeetingErrorCode.INVALID_MEETING_DATE_RANGE)
        }
}
