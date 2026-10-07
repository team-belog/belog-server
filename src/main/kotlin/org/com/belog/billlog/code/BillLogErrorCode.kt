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
    BILL_NOT_FOUND(HttpStatus.NOT_FOUND, "BILL_LOG-E011", "결제 내역을 찾을 수 없습니다."),
    MEETING_DATE_NOT_CONFIRMED(HttpStatus.CONFLICT, "BILL_LOG-E012", "만남 일정이 확정되지 않아 결제 회차를 계산할 수 없습니다."),
    SETTLEMENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "BILL_LOG-E013", "정산 요청을 찾을 수 없습니다."),
    SETTLEMENT_REQUEST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "BILL_LOG-E014", "정산 요청 대상자만 완료할 수 있습니다."),
    INVALID_SETTLEMENT_REQUEST_CURSOR(HttpStatus.BAD_REQUEST, "BILL_LOG-E015", "정산 현황 커서가 올바르지 않습니다."),
    UNSUPPORTED_RECEIPT_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "BILL_LOG-E016", "지원하지 않는 영수증 이미지 형식입니다."),
    INVALID_RECEIPT_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "BILL_LOG-E017", "영수증 이미지는 10MB 이하여야 합니다."),
    INVALID_RECEIPT_IMAGE_OBJECT_KEY(HttpStatus.BAD_REQUEST, "BILL_LOG-E018", "영수증 이미지 경로가 올바르지 않습니다."),
    RECEIPT_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "BILL_LOG-E019", "영수증 이미지를 찾을 수 없습니다."),
    INVALID_RECEIPT_IMAGE_METADATA(HttpStatus.BAD_REQUEST, "BILL_LOG-E020", "영수증 이미지 정보가 올바르지 않습니다."),
    RECEIPT_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "BILL_LOG-E021", "영수증 이미지 분석에 실패했습니다."),
    RECEIPT_ANALYSIS_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "BILL_LOG-E022", "영수증 이미지 분석 시간이 초과되었습니다."),
    UNSETTLED_SETTLEMENT_REQUEST_EXISTS(HttpStatus.CONFLICT, "BILL_LOG-E023", "아직 정산이 완료되지 않았어요"),
}
