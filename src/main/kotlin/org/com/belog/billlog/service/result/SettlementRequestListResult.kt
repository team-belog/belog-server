package org.com.belog.billlog.service.result

import org.com.belog.billlog.domain.SettlementRequestStatus

enum class SettlementRequestAction {
    SEND_REMINDER,
    MARK_COMPLETE,
    NONE,
}

data class SettlementRequestListResult(
    val items: List<SettlementRequestListItemResult>,
    val nextCursor: String?,
    val hasNext: Boolean,
)

data class SettlementRequestListItemResult(
    val settlementRequestId: Long,
    val amount: Long,
    val sender: SettlementParticipantResult,
    val receiver: SettlementParticipantResult,
    val status: SettlementRequestStatus,
    val action: SettlementRequestAction,
)

data class SettlementParticipantResult(
    val meetingParticipantId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val isMe: Boolean,
)
