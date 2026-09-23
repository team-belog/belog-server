package org.com.belog.billlog.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class BillLogSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    BILL_REGISTERED(HttpStatus.CREATED, "BILL_LOG-S001", "결제 내역과 정산 요청이 등록되었습니다."),
}
