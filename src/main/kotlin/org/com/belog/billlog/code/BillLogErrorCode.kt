package org.com.belog.billlog.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class BillLogErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_BILL(HttpStatus.BAD_REQUEST, "BILL_LOG-E001", "결제 내역 정보가 올바르지 않습니다."),
    EMPTY_BILL_ITEM(HttpStatus.BAD_REQUEST, "BILL_LOG-E002", "결제 항목은 한 개 이상이어야 합니다."),
    EMPTY_BILL_SHARE(HttpStatus.BAD_REQUEST, "BILL_LOG-E003", "개인별 부담 금액은 한 명 이상 입력해야 합니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "BILL_LOG-E004", "결제 금액은 0원보다 커야 합니다."),
    ITEM_TOTAL_MISMATCH(HttpStatus.BAD_REQUEST, "BILL_LOG-E005", "결제 항목 합계가 결제 총액과 일치하지 않습니다."),
    SHARE_TOTAL_MISMATCH(HttpStatus.BAD_REQUEST, "BILL_LOG-E006", "개인별 부담 금액 합계가 결제 총액과 일치하지 않습니다."),
    DUPLICATE_SHARE_PARTICIPANT(HttpStatus.BAD_REQUEST, "BILL_LOG-E007", "개인별 부담 금액에 중복된 참여자가 있습니다."),
    PAYER_NOT_MEETING_PARTICIPANT(HttpStatus.BAD_REQUEST, "BILL_LOG-E008", "결제자는 해당 만남의 참여자여야 합니다."),
    INVALID_SHARE_PARTICIPANT(HttpStatus.BAD_REQUEST, "BILL_LOG-E009", "부담자는 해당 만남의 참여자여야 합니다."),
    AMOUNT_OVERFLOW(HttpStatus.BAD_REQUEST, "BILL_LOG-E010", "결제 금액 합계가 허용 범위를 초과했습니다."),
}
