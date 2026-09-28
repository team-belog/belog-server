package org.com.belog.prelog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.prelog.service.result.PlanPinResult

@Schema(description = "계획 핀 상태 변경 결과")
data class PlanPinResponse(
    @field:Schema(description = "계획 ID", example = "12")
    val planId: Long,
    @field:Schema(description = "핀 고정 여부", example = "true")
    val pinned: Boolean,
) {
    companion object {
        fun from(result: PlanPinResult): PlanPinResponse =
            PlanPinResponse(
                planId = result.planId,
                pinned = result.pinned,
            )
    }
}
