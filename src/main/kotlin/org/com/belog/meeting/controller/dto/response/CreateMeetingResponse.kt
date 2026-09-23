package org.com.belog.meeting.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.meeting.service.result.CreatedMeeting

@Schema(description = "만남 생성 결과")
data class CreateMeetingResponse(
    @field:Schema(description = "만남 ID", example = "1")
    val meetingId: Long,
) {
    companion object {
        fun from(createdMeeting: CreatedMeeting): CreateMeetingResponse =
            CreateMeetingResponse(
                meetingId = createdMeeting.meetingId,
            )
    }
}
