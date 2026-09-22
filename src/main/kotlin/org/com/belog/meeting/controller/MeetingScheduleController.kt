package org.com.belog.meeting.controller

import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.meeting.code.MeetingSuccessCode
import org.com.belog.meeting.controller.dto.request.ConfirmMeetingDateRequest
import org.com.belog.meeting.controller.dto.request.SubmitDatePollResponseRequest
import org.com.belog.meeting.controller.dto.response.MeetingDatePollResponse
import org.com.belog.meeting.controller.dto.response.MeetingDatePollResultsResponse
import org.com.belog.meeting.controller.swagger.MeetingScheduleSwagger
import org.com.belog.meeting.service.MeetingDatePollService
import org.com.belog.meeting.service.MeetingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/meetings/{meetingId}")
class MeetingScheduleController(
    private val meetingService: MeetingService,
    private val meetingDatePollService: MeetingDatePollService,
) : MeetingScheduleSwagger {
    @GetMapping("/date-poll")
    override fun getDatePoll(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<CommonResponse<MeetingDatePollResponse>> {
        val result = meetingDatePollService.getDatePoll(meetingId, userId)

        return ResponseEntity
            .status(MeetingSuccessCode.DATE_POLL_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.DATE_POLL_RETRIEVED,
                    MeetingDatePollResponse.from(result),
                ),
            )
    }

    @GetMapping("/date-poll/results")
    override fun getDatePollResults(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
    ): ResponseEntity<CommonResponse<MeetingDatePollResultsResponse>> {
        val result = meetingDatePollService.getDatePollResults(meetingId, userId)

        return ResponseEntity
            .status(MeetingSuccessCode.DATE_POLL_RESULTS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    MeetingSuccessCode.DATE_POLL_RESULTS_RETRIEVED,
                    MeetingDatePollResultsResponse.from(result),
                ),
            )
    }

    @PutMapping("/date-poll/responses/me")
    override fun submitMyDatePollResponse(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: SubmitDatePollResponseRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        meetingDatePollService.respondDatePoll(
            meetingId = meetingId,
            userId = userId,
            candidateDateRangeIds = request.candidateDateRangeIds,
        )

        return ResponseEntity
            .status(MeetingSuccessCode.DATE_POLL_RESPONSE_SUBMITTED.status)
            .body(CommonResponse.success(MeetingSuccessCode.DATE_POLL_RESPONSE_SUBMITTED))
    }

    @PutMapping("/confirmed-date")
    override fun confirmMeetingDate(
        @LoginUserId userId: Long,
        @PathVariable meetingId: Long,
        @Valid @RequestBody request: ConfirmMeetingDateRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        meetingService.confirmMeetingDate(
            meetingId = meetingId,
            userId = userId,
            candidateDateRangeId = request.candidateDateRangeId,
        )

        return ResponseEntity
            .status(MeetingSuccessCode.MEETING_DATE_CONFIRMED.status)
            .body(CommonResponse.success(MeetingSuccessCode.MEETING_DATE_CONFIRMED))
    }
}
