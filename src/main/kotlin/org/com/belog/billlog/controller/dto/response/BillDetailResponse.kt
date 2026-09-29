package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.service.result.BillDetailResult
import org.com.belog.billlog.service.result.BillItemResult
import org.com.belog.billlog.service.result.BillShareResult
import java.time.LocalDate

@Schema(description = "결제 내역 상세 조회 결과")
data class BillDetailResponse(
    @field:Schema(description = "결제 내역 ID", example = "45")
    val billId: Long,
    @field:Schema(
        description = "만남 시작일을 1일 차로 계산한 결제 회차. 시작일 이전은 -1, -2 순으로 계산",
        example = "1",
    )
    val dayNumber: Int,
    @field:Schema(description = "결제 내역 등록일", example = "2026-08-06")
    val paymentDate: LocalDate,
    @field:Schema(description = "결제 내역 제목", example = "아랑이 카페")
    val title: String,
    @field:Schema(description = "결제자 닉네임", example = "정바미")
    val payerNickname: String,
    @field:Schema(
        description = "정산 방식",
        example = "EQUAL_SPLIT",
        allowableValues = ["EQUAL_SPLIT", "ITEM_TAG", "MANUAL"],
    )
    val settlementMethod: BillSplitType,
    @field:ArraySchema(
        arraySchema = Schema(description = "결제 항목 목록"),
        schema = Schema(implementation = BillItemResponse::class),
    )
    val items: List<BillItemResponse>,
    @field:Schema(description = "총 결제 금액", example = "11000")
    val totalAmount: Long,
    @field:ArraySchema(
        arraySchema = Schema(description = "개인별 부담 금액 목록"),
        schema = Schema(implementation = BillShareResponse::class),
    )
    val shares: List<BillShareResponse>,
) {
    companion object {
        fun from(result: BillDetailResult): BillDetailResponse =
            BillDetailResponse(
                billId = result.billId,
                dayNumber = result.dayNumber,
                paymentDate = result.paymentDate,
                title = result.title,
                payerNickname = result.payerNickname,
                settlementMethod = result.settlementMethod,
                items = result.items.map(BillItemResponse::from),
                totalAmount = result.totalAmount,
                shares = result.shares.map(BillShareResponse::from),
            )
    }
}

@Schema(description = "결제 항목")
data class BillItemResponse(
    @field:Schema(description = "결제 항목명", example = "아메리카노")
    val name: String,
    @field:Schema(description = "결제 항목 금액", example = "4000")
    val amount: Long,
) {
    companion object {
        fun from(result: BillItemResult): BillItemResponse =
            BillItemResponse(
                name = result.name,
                amount = result.amount,
            )
    }
}

@Schema(description = "개인별 부담 금액")
data class BillShareResponse(
    @field:Schema(description = "만남 참여자 ID", example = "31")
    val meetingParticipantId: Long,
    @field:Schema(description = "참여자 닉네임", example = "정바미")
    val nickname: String,
    @field:Schema(
        description = "조회 가능한 프로필 이미지 URL. 기본 이미지 사용 시 null",
        example = "https://belog-storage.s3.ap-northeast-2.amazonaws.com/users/15/profile/image.webp?...",
        nullable = true,
    )
    val profileImageUrl: String?,
    @field:Schema(description = "참여자 부담 금액", example = "4000")
    val amount: Long,
    @field:Schema(description = "결제자 여부", example = "true")
    val payer: Boolean,
) {
    companion object {
        fun from(result: BillShareResult): BillShareResponse =
            BillShareResponse(
                meetingParticipantId = result.meetingParticipantId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
                amount = result.amount,
                payer = result.payer,
            )
    }
}
