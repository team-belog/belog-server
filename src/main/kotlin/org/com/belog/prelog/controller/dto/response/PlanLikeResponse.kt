package org.com.belog.prelog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.prelog.service.result.PlanLikeResult

@Schema(description = "계획 좋아요 상태 변경 결과")
data class PlanLikeResponse(
    @field:Schema(description = "계획 ID", example = "12")
    val planId: Long,
    @field:Schema(description = "로그인 사용자의 좋아요 여부", example = "true")
    val likedByMe: Boolean,
    @field:Schema(description = "전체 좋아요 수", example = "3")
    val likeCount: Long,
) {
    companion object {
        fun from(result: PlanLikeResult): PlanLikeResponse =
            PlanLikeResponse(
                planId = result.planId,
                likedByMe = result.likedByMe,
                likeCount = result.likeCount,
            )
    }
}
