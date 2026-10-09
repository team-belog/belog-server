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
    BILL_LIST_RETRIEVED(HttpStatus.OK, "BILL_LOG-S005", "결제 내역을 조회했습니다."),
    RECEIPT_IMAGE_UPLOAD_URL_ISSUED(HttpStatus.OK, "BILL_LOG-S006", "영수증 이미지 업로드 URL이 발급되었습니다."),
    RECEIPT_ANALYZED(HttpStatus.OK, "BILL_LOG-S007", "영수증 이미지를 분석했습니다."),
    SETTLEMENT_REMINDER_SENT(HttpStatus.NO_CONTENT, "BILL_LOG-S008", "정산 리마인드를 전송했습니다."),
}
