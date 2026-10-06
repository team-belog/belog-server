package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.service.result.ReceiptAnalysisItemResult
import org.com.belog.billlog.service.result.ReceiptAnalysisResult
import java.time.LocalDate

@Schema(description = "수정 가능한 영수증 이미지 분석 결과")
data class ReceiptAnalysisResponse(
    @field:Schema(description = "영수증 제목 또는 상호명", example = "벨로그 식당", nullable = true)
    val title: String?,
    @field:Schema(description = "결제일", example = "2026-10-06", nullable = true)
    val paymentDate: LocalDate?,
    @field:ArraySchema(
        arraySchema = Schema(description = "추출된 결제 항목 목록"),
        schema = Schema(implementation = ReceiptAnalysisItemResponse::class),
    )
    val items: List<ReceiptAnalysisItemResponse>,
    @field:Schema(description = "영수증에 표시된 총액", example = "29000", nullable = true)
    val totalAmount: Long?,
    @field:Schema(
        description = "항목 또는 총액이 누락되었거나 항목 합계와 총액이 달라 사용자 확인이 필요한지 여부",
        example = "false",
    )
    val requiresConfirmation: Boolean,
) {
    companion object {
        fun from(result: ReceiptAnalysisResult): ReceiptAnalysisResponse =
            ReceiptAnalysisResponse(
                title = result.title,
                paymentDate = result.paymentDate,
                items = result.items.map(ReceiptAnalysisItemResponse::from),
                totalAmount = result.totalAmount,
                requiresConfirmation = result.requiresConfirmation,
            )
    }
}

@Schema(description = "영수증 결제 항목 분석 결과")
data class ReceiptAnalysisItemResponse(
    @field:Schema(description = "결제 항목명", example = "파스타", nullable = true)
    val name: String?,
    @field:Schema(description = "결제 항목 금액", example = "15000", nullable = true)
    val amount: Long?,
) {
    companion object {
        fun from(result: ReceiptAnalysisItemResult): ReceiptAnalysisItemResponse =
            ReceiptAnalysisItemResponse(
                name = result.name,
                amount = result.amount,
            )
    }
}
