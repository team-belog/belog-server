package org.com.belog.prelog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.service.result.PlanListItemResult
import org.com.belog.prelog.service.result.PlanListResult
import java.time.Instant

@Schema(description = "Pre-log 계획 목록 조회 결과")
data class PlanListResponse(
    @field:Schema(description = "계획 목록")
    val items: List<PlanListItemResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", example = "101", nullable = true)
    val nextCursor: Long?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: PlanListResult): PlanListResponse =
            PlanListResponse(
                items = result.items.map(PlanListItemResponse::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "Pre-log 계획 목록 항목")
data class PlanListItemResponse(
    @field:Schema(description = "계획 ID", example = "121")
    val planId: Long,
    @field:Schema(description = "계획 유형", example = "LINK", allowableValues = ["LINK", "MEMO"])
    val type: PlanType,
    @field:Schema(description = "계획 카테고리", example = "ACCOMMODATION")
    val category: PlanCategory,
    @field:Schema(description = "계획 제목", example = "광주 숙소")
    val title: String,
    @field:Schema(description = "LINK 유형의 이동 URL. MEMO 유형이면 null", nullable = true)
    val url: String?,
    @field:Schema(description = "추출된 주소", nullable = true)
    val address: String?,
    @field:Schema(description = "추출된 썸네일 URL", nullable = true)
    val thumbnailUrl: String?,
    @field:Schema(description = "좋아요 수", example = "3")
    val likeCount: Long,
    @field:Schema(description = "로그인 사용자의 좋아요 여부", example = "true")
    val likedByMe: Boolean,
    @field:Schema(description = "고정 여부", example = "true")
    val pinned: Boolean,
    @field:Schema(description = "로그인 사용자의 삭제 권한 여부", example = "true")
    val canDelete: Boolean,
    @field:Schema(description = "계획 생성 시각", example = "2026-09-22T10:30:00Z")
    val createdAt: Instant,
) {
    companion object {
        fun from(result: PlanListItemResult): PlanListItemResponse =
            PlanListItemResponse(
                planId = result.planId,
                type = result.type,
                category = result.category,
                title = result.title,
                url = result.url,
                address = result.address,
                thumbnailUrl = result.thumbnailUrl,
                likeCount = result.likeCount,
                likedByMe = result.likedByMe,
                pinned = result.pinned,
                canDelete = result.canDelete,
                createdAt = result.createdAt,
            )
    }
}
