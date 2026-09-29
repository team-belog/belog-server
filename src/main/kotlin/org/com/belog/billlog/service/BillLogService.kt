package org.com.belog.billlog.service

import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.result.BillLogSummaryResult
import org.com.belog.billlog.service.result.SettlementParticipantResult
import org.com.belog.billlog.service.result.SettlementRequestAction
import org.com.belog.billlog.service.result.SettlementRequestListItemResult
import org.com.belog.billlog.service.result.SettlementRequestListResult
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.user.domain.User
import org.com.belog.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.util.Base64

@Service
class BillLogService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val billRepository: BillRepository,
    private val settlementRequestRepository: SettlementRequestRepository,
    private val userService: UserService,
) {
    @Transactional(readOnly = true)
    fun getSummary(
        meetingId: Long,
        userId: Long,
    ): BillLogSummaryResult {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Bill-log 대상 만남의 그룹 ID가 없습니다." }

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

        return BillLogSummaryResult(
            totalSpentAmount = billRepository.sumTotalAmountByMeetingId(meetingId),
            completedParticipantCount =
                settlementRequestRepository.countCompletedParticipantsByMeetingId(
                    meetingId = meetingId,
                    pendingStatus = SettlementRequestStatus.PENDING,
                ),
            pendingParticipantCount =
                settlementRequestRepository.countDistinctParticipantsByMeetingIdAndStatus(
                    meetingId = meetingId,
                    status = SettlementRequestStatus.PENDING,
                ),
        )
    }

    @Transactional(readOnly = true)
    fun getSettlementRequests(
        meetingId: Long,
        userId: Long,
        cursor: String?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): SettlementRequestListResult {
        require(size in 1..MAX_PAGE_SIZE) { "조회 개수는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다." }

        validateGroupMember(meetingId, userId)
        val decodedCursor = cursor?.let(::decodeSettlementRequestCursor)

        val settlementRequests =
            settlementRequestRepository.findPageWithParticipants(
                meetingId = meetingId,
                cursorStatus = decodedCursor?.status,
                cursorId = decodedCursor?.settlementRequestId,
                pendingStatus = SettlementRequestStatus.PENDING,
                completedStatus = SettlementRequestStatus.COMPLETED,
                pageable = PageRequest.of(0, size + 1),
            )
        val hasNext = settlementRequests.size > size
        val pageRequests = if (hasNext) settlementRequests.take(size) else settlementRequests

        return SettlementRequestListResult(
            items = pageRequests.map { settlementRequest -> settlementRequest.toListItemResult(userId) },
            nextCursor = pageRequests.lastOrNull()?.takeIf { hasNext }?.let(::encodeSettlementRequestCursor),
            hasNext = hasNext,
        )
    }

    private fun validateGroupMember(
        meetingId: Long,
        userId: Long,
    ) {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Bill-log 대상 만남의 그룹 ID가 없습니다." }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }
    }

    private fun SettlementRequest.toListItemResult(userId: Long): SettlementRequestListItemResult {
        val senderParticipant = participant
        val receiverParticipant = bill.payer
        val senderUser = senderParticipant.groupMember.user
        val receiverUser = receiverParticipant.groupMember.user
        val senderUserId = checkNotNull(senderUser.id) { "송금자의 사용자 ID가 없습니다." }
        val receiverUserId = checkNotNull(receiverUser.id) { "수취인의 사용자 ID가 없습니다." }

        val action =
            when {
                status != SettlementRequestStatus.PENDING -> SettlementRequestAction.NONE
                userId == receiverUserId -> SettlementRequestAction.SEND_REMINDER
                userId == senderUserId -> SettlementRequestAction.MARK_COMPLETE
                else -> SettlementRequestAction.NONE
            }

        return SettlementRequestListItemResult(
            settlementRequestId = checkNotNull(id) { "조회된 정산 요청의 ID가 없습니다." },
            amount = amount,
            sender = senderParticipant.toResult(senderUser, senderUserId == userId),
            receiver = receiverParticipant.toResult(receiverUser, receiverUserId == userId),
            status = status,
            action = action,
        )
    }

    private fun encodeSettlementRequestCursor(settlementRequest: SettlementRequest): String {
        val settlementRequestId = checkNotNull(settlementRequest.id) { "커서 대상 정산 요청의 ID가 없습니다." }
        val rawCursor = "${settlementRequest.status.name}:$settlementRequestId"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.toByteArray(StandardCharsets.UTF_8))
    }

    private fun decodeSettlementRequestCursor(cursor: String): SettlementRequestCursor =
        try {
            val decoded = String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8)
            val parts = decoded.split(CURSOR_SEPARATOR, limit = CURSOR_PART_COUNT)
            require(parts.size == CURSOR_PART_COUNT)
            val status = SettlementRequestStatus.valueOf(parts[0])
            val settlementRequestId = parts[1].toLong()
            require(settlementRequestId > 0)
            SettlementRequestCursor(status = status, settlementRequestId = settlementRequestId)
        } catch (exception: IllegalArgumentException) {
            throw IllegalArgumentException("정산 현황 커서가 올바르지 않습니다.", exception)
        }

    private fun MeetingParticipant.toResult(
        user: User,
        isMe: Boolean,
    ): SettlementParticipantResult =
        SettlementParticipantResult(
            meetingParticipantId = checkNotNull(id) { "조회된 만남 참여자의 ID가 없습니다." },
            nickname = checkNotNull(user.nickname) { "조회된 참여자의 닉네임이 없습니다." },
            profileImageUrl = userService.resolveProfileImageUrl(user),
            isMe = isMe,
        )

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
        private const val CURSOR_SEPARATOR = ":"
        private const val CURSOR_PART_COUNT = 2
    }

    private data class SettlementRequestCursor(
        val status: SettlementRequestStatus,
        val settlementRequestId: Long,
    )
}
