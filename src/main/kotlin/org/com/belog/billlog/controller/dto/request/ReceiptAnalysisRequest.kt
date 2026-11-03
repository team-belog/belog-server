package org.com.belog.billlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.billlog.domain.ReceiptImageObjectKey

data class ReceiptAnalysisRequest(
    @field:Schema(
        description = "분석할 영수증 이미지의 S3 객체 키",
        example = "bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg",
    )
    @field:NotBlank(message = "영수증 이미지 object key를 입력해 주세요.")
    @field:Size(
        max = ReceiptImageObjectKey.MAX_LENGTH,
        message = "영수증 이미지 object key는 ${ReceiptImageObjectKey.MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    val receiptImageObjectKey: String,
)
