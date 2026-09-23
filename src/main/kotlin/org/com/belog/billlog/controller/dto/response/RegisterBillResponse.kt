package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.service.result.RegisteredBill

@Schema(description = "결제 내역 및 정산 요청 등록 결과")
data class RegisterBillResponse(
    @field:Schema(description = "결제 내역 ID", example = "1")
    val billId: Long,
) {
    companion object {
        fun from(registeredBill: RegisteredBill): RegisterBillResponse =
            RegisterBillResponse(
                billId = registeredBill.billId,
            )
    }
}
