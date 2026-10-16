package org.com.belog.prelog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory

@Schema(description = "Pre-log 메모 계획 수정 요청")
data class UpdatePlanRequest(
    @field:Schema(
        description = "계획 카테고리",
        example = "RESTAURANT",
        allowableValues = [
            "RESTAURANT",
            "CAFE",
            "ACCOMMODATION",
            "ACTIVITY",
            "TRANSPORTATION",
            "SHOPPING",
            "OTHER",
        ],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val category: PlanCategory,
    @field:Schema(
        description = "계획 제목",
        example = "광주 맛집",
        minLength = Plan.TITLE_MIN_LENGTH,
        maxLength = Plan.TITLE_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "계획 제목은 공백일 수 없습니다.")
    @field:Size(
        min = Plan.TITLE_MIN_LENGTH,
        max = Plan.TITLE_MAX_LENGTH,
        message = "계획 제목은 ${Plan.TITLE_MIN_LENGTH}자 이상 ${Plan.TITLE_MAX_LENGTH}자 이하여야 합니다.",
    )
    val title: String,
    @field:Schema(
        description = "메모 본문",
        example = "웨이팅을 대비해 여유롭게 일정을 잡아야 함",
        minLength = Plan.CONTENT_MIN_LENGTH,
        maxLength = Plan.CONTENT_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "계획 내용은 공백일 수 없습니다.")
    @field:Size(
        min = Plan.CONTENT_MIN_LENGTH,
        max = Plan.CONTENT_MAX_LENGTH,
        message = "계획 내용은 ${Plan.CONTENT_MIN_LENGTH}자 이상 ${Plan.CONTENT_MAX_LENGTH}자 이하여야 합니다.",
    )
    val content: String,
)
