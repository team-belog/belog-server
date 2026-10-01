package org.com.belog.meeting.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.meeting.domain.Meeting
import java.time.LocalDate

@Schema(description = "만남 수정 요청")
data class UpdateMeetingRequest(
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
        description = "만남 장소. null이면 기존 장소를 삭제합니다.",
        example = "광주광역시 000 000",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = Meeting.LOCATION_MAX_LENGTH,
        nullable = true,
    )
    @field:Size(
        max = Meeting.LOCATION_MAX_LENGTH,
        message = "만남 장소는 ${Meeting.LOCATION_MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    val location: String?,
    @field:Schema(
        description = "확정 일정 시작일. 일정 조율 중인 만남은 null을 전달합니다.",
        example = "2026-10-03",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
    )
    val startDate: LocalDate?,
    @field:Schema(
        description = "확정 일정 종료일. 일정 조율 중인 만남은 null을 전달합니다.",
        example = "2026-10-04",
        requiredMode = Schema.RequiredMode.REQUIRED,
        nullable = true,
    )
    val endDate: LocalDate?,
) {
    @get:AssertTrue(message = "만남 시작일과 종료일은 모두 입력하거나 모두 입력하지 않아야 합니다.")
    @get:Schema(hidden = true)
    val isDateRangeComplete: Boolean
        get() = (startDate == null) == (endDate == null)
}
