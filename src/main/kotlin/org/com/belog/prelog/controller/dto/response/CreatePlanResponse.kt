package org.com.belog.prelog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType

@Schema(description = "Pre-log 계획 생성 결과")
data class CreatePlanResponse(
    @field:Schema(description = "계획 ID", example = "1")
    val planId: Long,
    @field:Schema(description = "만남 ID", example = "1")
    val meetingId: Long,
    @field:Schema(description = "계획을 작성한 그룹 멤버 ID", example = "10")
    val creatorGroupMemberId: Long,
    @field:Schema(description = "계획 유형", example = "LINK", allowableValues = ["LINK", "MEMO"])
    val type: PlanType,
    @field:Schema(description = "계획 카테고리", example = "RESTAURANT")
    val category: PlanCategory,
    @field:Schema(description = "계획 제목", example = "광주 맛집")
    val title: String,
    @field:Schema(description = "링크 URL. MEMO 유형이면 null", nullable = true)
    val url: String?,
    @field:Schema(description = "메모 내용. LINK 유형이면 null", nullable = true)
    val content: String?,
) {
    companion object {
        fun from(plan: Plan): CreatePlanResponse =
            CreatePlanResponse(
                planId = requireNotNull(plan.id),
                meetingId = requireNotNull(plan.meeting.id),
                creatorGroupMemberId = requireNotNull(plan.createdBy.id),
                type = plan.type,
                category = plan.category,
                title = plan.title,
                url = plan.url,
                content = plan.content,
            )
    }
}
