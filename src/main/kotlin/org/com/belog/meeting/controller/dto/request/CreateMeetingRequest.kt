package org.com.belog.meeting.controller.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
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
        description = "일정 등록 방식. 현재 확정 날짜 방식만 지원합니다.",
        example = "FIXED",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val type: MeetingScheduleType,
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
