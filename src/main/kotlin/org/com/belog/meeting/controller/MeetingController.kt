package org.com.belog.meeting.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.CommonResponse
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.code.MeetingSuccessCode
import org.com.belog.meeting.controller.dto.request.CreateMeetingRequest
import org.com.belog.meeting.controller.dto.response.CreateMeetingResponse
import org.com.belog.meeting.controller.swagger.MeetingSwagger
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.service.MeetingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups/{groupId}/meetings")
class MeetingController(
    private val meetingService: MeetingService,
) : MeetingSwagger {
    @PostMapping
    override fun createMeeting(
        @LoginUserId userId: Long,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateMeetingRequest,
    ): ResponseEntity<CommonResponse<CreateMeetingResponse>> {
        if (request.schedule.type != MeetingScheduleType.FIXED) {
            throw BusinessException(MeetingErrorCode.UNSUPPORTED_SCHEDULE_TYPE)
        }

        val createdMeeting =
            meetingService.createFixedMeeting(
                groupId = groupId,
                creatorUserId = userId,
                name = request.name,
                location = request.location,
                participantMemberIds = request.participantMemberIds,
                startDate = request.schedule.startDate,
                endDate = request.schedule.endDate,
            )

        return ResponseEntity
            .status(MeetingSuccessCode.MEETING_CREATED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.MEETING_CREATED,
                    CreateMeetingResponse.from(createdMeeting),
                ),
            )
    }
}
