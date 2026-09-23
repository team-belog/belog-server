package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.service.result.RegisteredBill

@Schema(description = "결제 내역 및 정산 요청 등록 결과")
data class RegisterBillResponse(
    @field:Schema(description = "결제 내역 ID", example = "1")
    val billId: Long,
    @field:Schema(description = "만남 ID", example = "7")
    val meetingId: Long,
    @field:Schema(description = "결제 총액", example = "11000")
    val totalAmount: Long,
    @field:Schema(
        description = "정산 방식",
        example = "EQUAL_SPLIT",
        allowableValues = ["EQUAL_SPLIT", "ITEM_TAG", "MANUAL"],
    )
    val splitType: BillSplitType,
    @field:Schema(description = "저장된 결제 항목 수", example = "2")
    val itemCount: Int,
    @field:Schema(description = "저장된 개인별 부담 금액 수", example = "2")
    val shareCount: Int,
    @field:Schema(description = "생성된 정산 요청 수. 결제자는 제외됩니다.", example = "1")
    val settlementRequestCount: Int,
) {
    companion object {
        fun from(registeredBill: RegisteredBill): RegisterBillResponse =
            RegisterBillResponse(
                billId = registeredBill.billId,
                meetingId = registeredBill.meetingId,
                totalAmount = registeredBill.totalAmount,
                splitType = registeredBill.splitType,
                itemCount = registeredBill.itemCount,
                shareCount = registeredBill.shareCount,
                settlementRequestCount = registeredBill.settlementRequestCount,
            )
    }
}
