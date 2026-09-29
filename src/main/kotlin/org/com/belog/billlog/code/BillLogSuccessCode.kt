package org.com.belog.billlog.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class BillLogSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    BILL_REGISTERED(HttpStatus.CREATED, "BILL_LOG-S001", "결제 내역과 정산 요청이 등록되었습니다."),
    BILL_DETAIL_RETRIEVED(HttpStatus.OK, "BILL_LOG-S002", "결제 내역 상세를 조회했습니다."),
    BILL_LOG_SUMMARY_RETRIEVED(HttpStatus.OK, "BILL_LOG-S003", "Bill-log 요약 정보를 조회했습니다."),
    SETTLEMENT_REQUEST_LIST_RETRIEVED(HttpStatus.OK, "BILL_LOG-S004", "정산 현황을 조회했습니다."),
}
