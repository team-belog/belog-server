package org.com.belog.billlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import org.com.belog.billlog.domain.SettlementRequestStatus

@Schema(description = "정산 요청 상태 변경 요청")
data class UpdateSettlementRequestStatusRequest(
    @field:Schema(
        description = "변경할 정산 상태",
        example = "COMPLETED",
        allowableValues = ["COMPLETED"],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotNull(message = "정산 요청 상태는 필수입니다.")
    val status: SettlementRequestStatus?,
)
