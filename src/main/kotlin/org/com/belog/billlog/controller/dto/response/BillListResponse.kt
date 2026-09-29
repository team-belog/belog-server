package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.service.result.BillDayResult
import org.com.belog.billlog.service.result.BillListItemResult
import org.com.belog.billlog.service.result.BillListResult
import java.time.LocalDate

@Schema(description = "결제 내역 목록 조회 결과")
data class BillListResponse(
    @field:Schema(description = "결제일별 결제 내역")
    val days: List<BillDayResponse>,
    @field:Schema(description = "다음 페이지 날짜 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursorDate: LocalDate?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: BillListResult): BillListResponse =
            BillListResponse(
                days = result.days.map(BillDayResponse::from),
                nextCursorDate = result.nextCursorDate,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "결제일별 결제 내역")
data class BillDayResponse(
    @field:Schema(description = "만남 시작일을 1일 차로 계산한 일차", example = "1")
    val dayNumber: Int,
    @field:Schema(description = "결제일", example = "2026-08-06")
    val paymentDate: LocalDate,
    @field:Schema(description = "해당 날짜의 총 결제 금액", example = "15000")
    val dailyTotalAmount: Long,
    @field:Schema(description = "해당 날짜의 결제 내역")
    val bills: List<BillListItemResponse>,
) {
    companion object {
        fun from(result: BillDayResult): BillDayResponse =
            BillDayResponse(
                dayNumber = result.dayNumber,
                paymentDate = result.paymentDate,
                dailyTotalAmount = result.dailyTotalAmount,
                bills = result.bills.map(BillListItemResponse::from),
            )
    }
}

@Schema(description = "결제 내역 목록 항목")
data class BillListItemResponse(
    @field:Schema(description = "결제 내역 ID", example = "45")
    val billId: Long,
    @field:Schema(description = "결제 내역 제목", example = "아랑이 카페")
    val title: String,
    @field:Schema(description = "결제자 닉네임", example = "정바미")
    val payerNickname: String,
    @field:Schema(description = "결제 금액", example = "7500")
    val totalAmount: Long,
) {
    companion object {
        fun from(result: BillListItemResult): BillListItemResponse =
            BillListItemResponse(
                billId = result.billId,
                title = result.title,
                payerNickname = result.payerNickname,
                totalAmount = result.totalAmount,
            )
    }
}
