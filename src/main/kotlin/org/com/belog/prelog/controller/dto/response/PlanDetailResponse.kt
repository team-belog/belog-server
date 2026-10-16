package org.com.belog.prelog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.service.result.PlanDetailResult

@Schema(description = "Pre-log 메모 계획 상세 조회 결과")
data class PlanDetailResponse(
    @field:Schema(description = "계획 ID", example = "12")
    val planId: Long,
    @field:Schema(description = "계획 카테고리", example = "OTHER")
    val category: PlanCategory,
    @field:Schema(description = "메모 제목", example = "준비물")
    val title: String,
    @field:Schema(description = "메모 본문 전체", example = "우산 챙기기")
    val content: String,
    @field:Schema(description = "로그인 사용자가 메모 작성자인 경우에만 true인 수정 권한 여부", example = "true")
    val canEdit: Boolean,
) {
    companion object {
        fun from(result: PlanDetailResult): PlanDetailResponse =
            PlanDetailResponse(
                planId = result.planId,
                category = result.category,
                title = result.title,
                content = result.content,
                canEdit = result.canEdit,
            )
    }
}
