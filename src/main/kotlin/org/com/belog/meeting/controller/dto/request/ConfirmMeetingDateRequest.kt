package org.com.belog.meeting.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Positive

@Schema(description = "후보 일정 확정 요청")
data class ConfirmMeetingDateRequest(
    @field:Positive
    @field:Schema(
        description = "확정할 후보 일정 범위 ID",
        example = "101",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val candidateDateRangeId: Long,
)
