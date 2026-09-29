package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.domain.calculateBillDayNumber
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.result.BillDayResult
import org.com.belog.billlog.service.result.BillListItemResult
import org.com.belog.billlog.service.result.BillListResult
import org.com.belog.billlog.service.result.BillLogSummaryResult
import org.com.belog.billlog.service.result.SettlementParticipantResult
import org.com.belog.billlog.service.result.SettlementRequestAction
import org.com.belog.billlog.service.result.SettlementRequestListItemResult
import org.com.belog.billlog.service.result.SettlementRequestListResult
import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.atStartOfBusinessDay
import org.com.belog.global.time.toBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.user.domain.User
import org.com.belog.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.LocalDate
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
        findAccessibleMeeting(meetingId, userId)

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
    fun getBills(
        meetingId: Long,
        userId: Long,
        cursorDate: LocalDate?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): BillListResult {
        require(size in 1..MAX_PAGE_SIZE) { "조회 날짜 수는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다." }

        val meeting = findAccessibleMeeting(meetingId, userId)
        val meetingStartDate =
            meeting.startDate
                ?: throw BusinessException(BillLogErrorCode.MEETING_DATE_NOT_CONFIRMED)
        val paymentDates =
            billRepository.findPaymentDatePage(
                meetingId = meetingId,
                cursorDate = cursorDate,
                pageable = PageRequest.of(0, size + 1),
            )
        val hasNext = paymentDates.size > size
        val pageDates = if (hasNext) paymentDates.take(size) else paymentDates

        if (pageDates.isEmpty()) {
            return BillListResult(days = emptyList(), nextCursorDate = null, hasNext = false)
        }

        val oldestDate = pageDates.last()
        val newestDate = pageDates.first()
        val billsByDate =
            billRepository
                .findAllWithPayerByMeetingIdAndCreatedAtRange(
                    meetingId = meetingId,
                    fromInclusive = oldestDate.atStartOfBusinessDay(),
                    toExclusive = newestDate.plusDays(1).atStartOfBusinessDay(),
                ).groupBy { bill ->
                    checkNotNull(bill.createdAt) { "조회된 결제 내역의 생성 시각이 없습니다." }.toBusinessDate()
                }

        return BillListResult(
            days =
                pageDates.map { paymentDate ->
                    val bills = billsByDate[paymentDate].orEmpty()
                    BillDayResult(
                        dayNumber = calculateBillDayNumber(meetingStartDate, paymentDate),
                        paymentDate = paymentDate,
                        dailyTotalAmount = sumAmounts(bills.map { bill -> bill.totalAmount }),
                        bills =
                            bills.map { bill ->
                                BillListItemResult(
                                    billId = checkNotNull(bill.id) { "조회된 결제 내역의 ID가 없습니다." },
                                    title = bill.title,
                                    payerNickname =
                                        checkNotNull(bill.payer.groupMember.user.nickname) {
                                            "조회된 결제자의 닉네임이 없습니다."
                                        },
                                    totalAmount = bill.totalAmount,
                                )
                            },
                    )
                },
            nextCursorDate = pageDates.lastOrNull()?.takeIf { hasNext },
            hasNext = hasNext,
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

        findAccessibleMeeting(meetingId, userId)
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

    private fun findAccessibleMeeting(
        meetingId: Long,
        userId: Long,
    ): Meeting {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Bill-log 대상 만남의 그룹 ID가 없습니다." }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }
        return meeting
    }

    private fun sumAmounts(amounts: List<Long>): Long =
        try {
            amounts.fold(0L) { total, amount -> Math.addExact(total, amount) }
        } catch (_: ArithmeticException) {
            throw BusinessException(BillLogErrorCode.AMOUNT_OVERFLOW)
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
            throw BusinessException(BillLogErrorCode.INVALID_SETTLEMENT_REQUEST_CURSOR, exception)
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
