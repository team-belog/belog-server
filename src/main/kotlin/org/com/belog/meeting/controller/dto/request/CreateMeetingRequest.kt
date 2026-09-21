package org.com.belog.meeting.controller.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.group.domain.Group
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingScheduleType
import java.time.LocalDate

data class CreateMeetingRequest(
    @field:Schema(
        description = "만남명",
        example = "광주 1박 2일",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = Meeting.NAME_MIN_LENGTH,
        maxLength = Meeting.NAME_MAX_LENGTH,
    )
    @field:NotBlank(message = "만남명은 공백일 수 없습니다.")
    @field:Size(
        min = Meeting.NAME_MIN_LENGTH,
        max = Meeting.NAME_MAX_LENGTH,
        message = "만남명은 ${Meeting.NAME_MIN_LENGTH}자 이상 ${Meeting.NAME_MAX_LENGTH}자 이하여야 합니다.",
    )
    val name: String,
    @field:Schema(
        description = "만남 장소",
        example = "서울고속버스터미널",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        maxLength = Meeting.LOCATION_MAX_LENGTH,
    )
    @field:Size(
        max = Meeting.LOCATION_MAX_LENGTH,
        message = "만남 장소는 ${Meeting.LOCATION_MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    val location: String? = null,
    @field:ArraySchema(
        arraySchema =
            Schema(
                description = "초대할 그룹 멤버 ID 목록. 생성자는 자동으로 참여자에 포함되므로 제외합니다.",
                example = "[22, 23]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            ),
        schema = Schema(implementation = Long::class),
        maxItems = Group.MAX_MEMBER_COUNT - CREATOR_COUNT,
    )
    @field:Size(
        max = Group.MAX_MEMBER_COUNT - CREATOR_COUNT,
        message = "초대할 수 있는 참여자는 최대 ${Group.MAX_MEMBER_COUNT - CREATOR_COUNT}명입니다.",
    )
    val participantMemberIds: List<Long> = emptyList(),
    @field:Valid
    @field:Schema(
        description = "일정 등록 정보",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val schedule: MeetingScheduleRequest,
) {
    companion object {
        private const val CREATOR_COUNT = 1
    }
}

data class MeetingScheduleRequest(
    @field:Schema(
        description = "일정 등록 방식",
        example = "FIXED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val type: MeetingScheduleType,
    @field:Valid
    @field:ArraySchema(
        arraySchema =
            Schema(
                description = "일정 범위 목록. FIXED는 1개, POLL은 2~10개를 전달합니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
            ),
        schema = Schema(implementation = MeetingDateRangeRequest::class),
        minItems = FIXED_DATE_RANGE_COUNT,
        maxItems = MAX_DATE_RANGE_COUNT,
    )
    @field:Size(
        min = FIXED_DATE_RANGE_COUNT,
        max = MAX_DATE_RANGE_COUNT,
        message = "일정 범위는 1개 이상 ${MAX_DATE_RANGE_COUNT}개 이하여야 합니다.",
    )
    val dateRanges: List<MeetingDateRangeRequest>,
) {
    @get:AssertTrue(message = "FIXED는 일정 범위 1개, POLL은 2개 이상 10개 이하를 입력해야 합니다.")
    @get:Schema(hidden = true)
    val isDateRangeCountValid: Boolean
        get() =
            when (type) {
                MeetingScheduleType.FIXED -> dateRanges.size == FIXED_DATE_RANGE_COUNT
                MeetingScheduleType.POLL -> dateRanges.size in MIN_POLL_DATE_RANGE_COUNT..MAX_DATE_RANGE_COUNT
            }

    companion object {
        private const val FIXED_DATE_RANGE_COUNT = 1
        private const val MIN_POLL_DATE_RANGE_COUNT = 2
        private const val MAX_DATE_RANGE_COUNT = 10
    }
}

data class MeetingDateRangeRequest(
    @field:Schema(
        description = "일정 시작일",
        example = "2026-10-03",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val startDate: LocalDate,
    @field:Schema(
        description = "일정 종료일. 하루 일정이면 시작일과 같은 날짜를 전달합니다.",
        example = "2026-10-04",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val endDate: LocalDate,
)
