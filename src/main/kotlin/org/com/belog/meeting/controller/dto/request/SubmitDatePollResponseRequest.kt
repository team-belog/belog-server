package org.com.belog.meeting.controller.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import org.com.belog.meeting.domain.MeetingCandidateDateRange

@Schema(description = "가능한 후보 일정 응답 요청")
data class SubmitDatePollResponseRequest(
    @field:ArraySchema(
        arraySchema =
            Schema(
                description = "참석 가능한 후보 일정 ID 목록. 빈 배열은 모든 후보 일정에 참석할 수 없음을 의미합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
            ),
        schema = Schema(implementation = Long::class),
        maxItems = MeetingCandidateDateRange.MAX_COUNT,
        uniqueItems = true,
    )
    @field:Size(
        max = MeetingCandidateDateRange.MAX_COUNT,
        message = "선택할 수 있는 후보 일정은 최대 ${MeetingCandidateDateRange.MAX_COUNT}개입니다.",
    )
    val candidateDateRangeIds: List<Long>,
)
