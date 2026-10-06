package org.com.belog.prelog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.prelog.controller.cursor.PlanListCursorCodec
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.service.result.MapPlanListItemResult
import org.com.belog.prelog.service.result.MapPlanListResult
import java.math.BigDecimal
import java.time.Instant

@Schema(description = "Pre-log 지도 계획 목록 조회 결과")
data class MapPlanListResponse(
    @field:Schema(description = "위치 정보가 있는 계획 목록")
    val items: List<MapPlanListItemResponse>,
    @field:Schema(description = "다음 페이지 커서", example = "MToxMDE", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: MapPlanListResult): MapPlanListResponse =
            MapPlanListResponse(
                items = result.items.map(MapPlanListItemResponse::from),
                nextCursor = result.nextCursor?.let(PlanListCursorCodec::encode),
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "Pre-log 지도 계획 목록 항목")
data class MapPlanListItemResponse(
    @field:Schema(description = "계획 ID", example = "121")
    val planId: Long,
    @field:Schema(description = "계획 카테고리", example = "RESTAURANT")
    val category: PlanCategory,
    @field:Schema(description = "계획 제목", example = "광주 맛집")
    val title: String,
    @field:Schema(description = "원본 링크 URL")
    val url: String,
    @field:Schema(description = "추출된 주소", nullable = true)
    val address: String?,
    @field:Schema(description = "위도", example = "37.5665000")
    val latitude: BigDecimal,
    @field:Schema(description = "경도", example = "126.9780000")
    val longitude: BigDecimal,
    @field:Schema(description = "고정 여부", example = "false")
    val pinned: Boolean,
    @field:Schema(description = "계획 생성 시각")
    val createdAt: Instant,
) {
    companion object {
        fun from(result: MapPlanListItemResult): MapPlanListItemResponse =
            MapPlanListItemResponse(
                planId = result.planId,
                category = result.category,
                title = result.title,
                url = result.url,
                address = result.address,
                latitude = result.latitude,
                longitude = result.longitude,
                pinned = result.pinned,
                createdAt = result.createdAt,
            )
    }
}
