package org.com.belog.prelog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.hibernate.validator.constraints.URL

@Schema(description = "Pre-log 계획 생성 요청")
data class CreatePlanRequest(
    @field:Schema(
        description = "계획 유형",
        example = "LINK",
        allowableValues = ["LINK", "MEMO"],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val type: PlanType,
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
        description = "LINK 유형에서 사용하는 HTTP 또는 HTTPS URL",
        example = "https://example.com/place",
        maxLength = Plan.URL_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    @field:Size(
        max = Plan.URL_MAX_LENGTH,
        message = "계획 URL은 ${Plan.URL_MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    @field:URL(message = "계획 URL은 유효한 URL이어야 합니다.")
    @field:Pattern(
        regexp = "(?i)^https?://.+$",
        message = "계획 URL은 HTTP 또는 HTTPS URL이어야 합니다.",
    )
    val url: String? = null,
    @field:Schema(
        description = "MEMO 유형에서 사용하는 내용",
        example = "웨이팅을 대비해 여유롭게 일정을 잡아야 함",
        minLength = Plan.CONTENT_MIN_LENGTH,
        maxLength = Plan.CONTENT_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.NOT_REQUIRED,
        nullable = true,
    )
    @field:Size(
        min = Plan.CONTENT_MIN_LENGTH,
        max = Plan.CONTENT_MAX_LENGTH,
        message = "계획 내용은 ${Plan.CONTENT_MIN_LENGTH}자 이상 ${Plan.CONTENT_MAX_LENGTH}자 이하여야 합니다.",
    )
    val content: String? = null,
) {
    @get:AssertTrue(message = "LINK는 URL만, MEMO는 내용만 입력해야 합니다.")
    @get:Schema(hidden = true)
    val isTypeFieldsValid: Boolean
        get() =
            when (type) {
                PlanType.LINK -> !url.isNullOrBlank() && content == null
                PlanType.MEMO -> !content.isNullOrBlank() && url == null
            }
}
