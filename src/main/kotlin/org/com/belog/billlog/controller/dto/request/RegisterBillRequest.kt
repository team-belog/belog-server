package org.com.belog.billlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.com.belog.billlog.domain.Bill
import org.com.belog.billlog.domain.BillAmount
import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.BillItem
import org.com.belog.billlog.domain.BillSplitType
import java.math.BigDecimal

@Schema(description = "결제 내역 및 정산 요청 등록 요청")
data class RegisterBillRequest(
    @field:Schema(
        description = "결제 내역 제목",
        example = "아랑이 카페",
        minLength = Bill.TITLE_MIN_LENGTH,
        maxLength = Bill.TITLE_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "결제 내역 제목은 공백일 수 없습니다.")
    @field:Size(
        min = Bill.TITLE_MIN_LENGTH,
        max = Bill.TITLE_MAX_LENGTH,
        message = "결제 내역 제목은 ${Bill.TITLE_MIN_LENGTH}자 이상 ${Bill.TITLE_MAX_LENGTH}자 이하여야 합니다.",
    )
    val title: String,
    @field:Schema(
        description = "결제한 그룹 멤버 ID",
        example = "10",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive(message = "결제자 그룹 멤버 ID는 양수여야 합니다.")
    val payerMemberId: Long,
    @field:Schema(
        description = "결제 화폐 단위. 생략하면 KRW\n- KRW: 원\n- USD: 달러\n- JPY: 엔\n- EUR: 유로",
        example = "KRW",
        allowableValues = ["KRW", "USD", "JPY", "EUR"],
        defaultValue = "KRW",
    )
    val currency: BillCurrency = BillCurrency.KRW,
    @field:Schema(
        description = "결제 총액. KRW와 JPY는 정수, USD와 EUR는 소수점 둘째 자리까지 입력할 수 있습니다.",
        example = "11000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive(message = "결제 총액은 0보다 커야 합니다.")
    @field:Digits(
        integer = BillAmount.MAX_INTEGER_DIGITS,
        fraction = BillAmount.SCALE,
        message = "금액은 정수 ${BillAmount.MAX_INTEGER_DIGITS}자리, 소수 ${BillAmount.SCALE}자리 이하여야 합니다.",
    )
    val totalAmount: BigDecimal,
    @field:Schema(
        description =
            "클라이언트에서 사용한 정산 방식\n" +
                "- EQUAL_SPLIT: 선택 인원 균등 분배\n" +
                "- ITEM_TAG: 항목별 태그 인원 분배\n" +
                "- MANUAL: 개인별 금액 직접 입력",
        example = "EQUAL_SPLIT",
        allowableValues = ["EQUAL_SPLIT", "ITEM_TAG", "MANUAL"],
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val splitType: BillSplitType,
    @field:Valid
    @field:NotEmpty(message = "결제 항목은 한 개 이상이어야 합니다.")
    @field:ArraySchema(
        arraySchema =
            Schema(
                description = "결제 항목 목록",
                requiredMode = Schema.RequiredMode.REQUIRED,
            ),
        schema = Schema(implementation = BillItemRequest::class),
        minItems = 1,
    )
    val items: List<BillItemRequest>,
    @field:Valid
    @field:NotEmpty(message = "개인별 부담 금액은 한 명 이상 입력해야 합니다.")
    @field:ArraySchema(
        arraySchema =
            Schema(
                description = "클라이언트가 확정한 개인별 부담 금액 목록. 배열 순서는 화면 표시 순서로 보존됩니다.",
                requiredMode = Schema.RequiredMode.REQUIRED,
            ),
        schema = Schema(implementation = BillShareRequest::class),
        minItems = 1,
    )
    val shares: List<BillShareRequest>,
)

@Schema(description = "결제 항목")
data class BillItemRequest(
    @field:Schema(
        description = "결제 항목명",
        example = "아메리카노",
        minLength = BillItem.NAME_MIN_LENGTH,
        maxLength = BillItem.NAME_MAX_LENGTH,
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank(message = "결제 항목명은 공백일 수 없습니다.")
    @field:Size(
        min = BillItem.NAME_MIN_LENGTH,
        max = BillItem.NAME_MAX_LENGTH,
        message = "결제 항목명은 ${BillItem.NAME_MIN_LENGTH}자 이상 ${BillItem.NAME_MAX_LENGTH}자 이하여야 합니다.",
    )
    val name: String,
    @field:Schema(
        description = "결제 항목 금액",
        example = "4000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive(message = "결제 항목 금액은 0보다 커야 합니다.")
    @field:Digits(
        integer = BillAmount.MAX_INTEGER_DIGITS,
        fraction = BillAmount.SCALE,
        message = "금액은 정수 ${BillAmount.MAX_INTEGER_DIGITS}자리, 소수 ${BillAmount.SCALE}자리 이하여야 합니다.",
    )
    val amount: BigDecimal,
)

@Schema(description = "개인별 최종 부담 금액")
data class BillShareRequest(
    @field:Schema(
        description = "부담자의 그룹 멤버 ID",
        example = "11",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive(message = "부담자 그룹 멤버 ID는 양수여야 합니다.")
    val participantMemberId: Long,
    @field:Schema(
        description = "최종 부담 금액",
        example = "7000",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:Positive(message = "개인별 부담 금액은 0보다 커야 합니다.")
    @field:Digits(
        integer = BillAmount.MAX_INTEGER_DIGITS,
        fraction = BillAmount.SCALE,
        message = "금액은 정수 ${BillAmount.MAX_INTEGER_DIGITS}자리, 소수 ${BillAmount.SCALE}자리 이하여야 합니다.",
    )
    val amount: BigDecimal,
)
