package org.com.belog.billlog.controller.dto.response

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.service.result.SettlementParticipantResult
import org.com.belog.billlog.service.result.SettlementRequestAction
import org.com.belog.billlog.service.result.SettlementRequestListItemResult
import org.com.belog.billlog.service.result.SettlementRequestListResult

@Schema(description = "진행 중인 정산 현황 조회 결과")
data class SettlementRequestListResponse(
    @field:Schema(description = "진행 중인 정산 요청 목록")
    val items: List<SettlementRequestListItemResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", example = "UEVORElORzoxMDE", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: SettlementRequestListResult): SettlementRequestListResponse =
            SettlementRequestListResponse(
                items = result.items.map(SettlementRequestListItemResponse::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "진행 중인 정산 요청")
data class SettlementRequestListItemResponse(
    @field:Schema(description = "정산 요청 ID", example = "101")
    val settlementRequestId: Long,
    @field:Schema(description = "정산 금액", example = "7500")
    val amount: Long,
    @field:Schema(description = "정산금을 보내는 참여자")
    val sender: SettlementParticipantResponse,
    @field:Schema(description = "정산금을 받는 참여자")
    val receiver: SettlementParticipantResponse,
    @field:Schema(
        description = "정산 상태",
        example = "PENDING",
        allowableValues = ["PENDING", "COMPLETED"],
    )
    val status: SettlementRequestStatus,
    @field:Schema(
        description = "로그인 사용자에게 허용된 화면 액션",
        example = "SEND_REMINDER",
        allowableValues = ["SEND_REMINDER", "MARK_COMPLETE", "NONE"],
    )
    val action: SettlementRequestAction,
) {
    companion object {
        fun from(result: SettlementRequestListItemResult): SettlementRequestListItemResponse =
            SettlementRequestListItemResponse(
                settlementRequestId = result.settlementRequestId,
                amount = result.amount,
                sender = SettlementParticipantResponse.from(result.sender),
                receiver = SettlementParticipantResponse.from(result.receiver),
                status = result.status,
                action = result.action,
            )
    }
}

@Schema(description = "정산 참여자")
data class SettlementParticipantResponse(
    @field:Schema(description = "만남 참여자 ID", example = "32")
    val meetingParticipantId: Long,
    @field:Schema(description = "닉네임", example = "바비")
    val nickname: String,
    @field:Schema(description = "조회 가능한 프로필 이미지 URL. 기본 이미지 사용 시 null", nullable = true)
    val profileImageUrl: String?,
    @field:Schema(description = "로그인 사용자 본인 여부", example = "false")
    @get:JsonProperty("isMe")
    val isMe: Boolean,
) {
    companion object {
        fun from(result: SettlementParticipantResult): SettlementParticipantResponse =
            SettlementParticipantResponse(
                meetingParticipantId = result.meetingParticipantId,
                nickname = result.nickname,
                profileImageUrl = result.profileImageUrl,
                isMe = result.isMe,
            )
    }
}
