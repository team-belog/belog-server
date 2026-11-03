package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.service.result.BillLogSummaryResult

@Schema(description = "Bill-log 요약 조회 결과")
data class BillLogSummaryResponse(
    @field:Schema(description = "해당 만남의 화폐 단위별 전체 결제 금액 합계. 결제 내역이 없으면 빈 목록. 화폐 단위 순서는 KRW, USD, JPY, EUR")
    val totalSpentAmounts: List<CurrencyAmountResponse>,
    @field:Schema(description = "본인의 모든 정산 요청을 완료한 참여자 수", example = "1")
    val completedParticipantCount: Long,
    @field:Schema(description = "완료하지 않은 정산 요청이 하나 이상 있는 참여자 수", example = "2")
    val pendingParticipantCount: Long,
) {
    companion object {
        fun from(result: BillLogSummaryResult): BillLogSummaryResponse =
            BillLogSummaryResponse(
                totalSpentAmounts = result.totalSpentAmounts.map(CurrencyAmountResponse::from),
                completedParticipantCount = result.completedParticipantCount,
                pendingParticipantCount = result.pendingParticipantCount,
            )
    }
}
