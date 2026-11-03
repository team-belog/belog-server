package org.com.belog.billlog.repository

import jakarta.persistence.LockModeType
import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SettlementRequestRepository : JpaRepository<SettlementRequest, Long> {
    @Query(
        """
        SELECT settlementRequest
        FROM SettlementRequest settlementRequest
        JOIN FETCH settlementRequest.receiver receiver
        JOIN FETCH receiver.groupMember receiverMember
        JOIN FETCH receiverMember.user
        JOIN FETCH settlementRequest.sender sender
        JOIN FETCH sender.groupMember senderMember
        JOIN FETCH senderMember.user
        WHERE settlementRequest.meeting.id = :meetingId
          AND settlementRequest.amount > 0
          AND (
              :cursorId IS NULL
              OR (
                  :cursorStatus = :pendingStatus
                  AND (
                      (settlementRequest.status = :pendingStatus AND settlementRequest.id < :cursorId)
                      OR settlementRequest.status = :completedStatus
                  )
              )
              OR (
                  :cursorStatus = :completedStatus
                  AND settlementRequest.status = :completedStatus
                  AND settlementRequest.id < :cursorId
              )
          )
        ORDER BY
            CASE WHEN settlementRequest.status = :pendingStatus THEN 0 ELSE 1 END ASC,
            settlementRequest.id DESC
        """,
    )
    fun findPageWithParticipants(
        @Param("meetingId") meetingId: Long,
        @Param("cursorStatus") cursorStatus: SettlementRequestStatus?,
        @Param("cursorId") cursorId: Long?,
        @Param("pendingStatus") pendingStatus: SettlementRequestStatus,
        @Param("completedStatus") completedStatus: SettlementRequestStatus,
        pageable: Pageable,
    ): List<SettlementRequest>

    @Query(
        """
        SELECT COUNT(DISTINCT settlementRequest.sender.id)
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.meeting.id = :meetingId
          AND settlementRequest.status = :status
        """,
    )
    fun countDistinctParticipantsByMeetingIdAndStatus(
        @Param("meetingId") meetingId: Long,
        @Param("status") status: SettlementRequestStatus,
    ): Long

    @Query(
        """
        SELECT COUNT(DISTINCT settlementRequest.sender.id)
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.meeting.id = :meetingId
          AND NOT EXISTS (
              SELECT pendingRequest.id
              FROM SettlementRequest pendingRequest
              WHERE pendingRequest.meeting.id = :meetingId
                AND pendingRequest.sender.id = settlementRequest.sender.id
                AND pendingRequest.status = :pendingStatus
          )
        """,
    )
    fun countCompletedParticipantsByMeetingId(
        @Param("meetingId") meetingId: Long,
        @Param("pendingStatus") pendingStatus: SettlementRequestStatus,
    ): Long

    @Query(
        value = """
        SELECT settlement_request.id
        FROM bill_log_settlement_requests settlement_request
        WHERE settlement_request.meeting_id = :meetingId
          AND settlement_request.status = :status
        LIMIT 1
        FOR UPDATE
        """,
        nativeQuery = true,
    )
    fun findFirstIdByMeetingIdAndStatusForUpdate(
        @Param("meetingId") meetingId: Long,
        @Param("status") status: String,
    ): Long?

    @Query(
        value = """
        SELECT settlement_request.id
        FROM bill_log_settlement_requests settlement_request
        JOIN meetings meeting ON meeting.id = settlement_request.meeting_id
        WHERE meeting.group_id = :groupId
          AND settlement_request.status = :status
        LIMIT 1
        FOR UPDATE
        """,
        nativeQuery = true,
    )
    fun findFirstIdByGroupIdAndStatusForUpdate(
        @Param("groupId") groupId: Long,
        @Param("status") status: String,
    ): Long?

    @Query(
        value = """
        SELECT settlement_request.id
        FROM bill_log_settlement_requests settlement_request
        JOIN meeting_participants sender ON sender.id = settlement_request.sender_meeting_participant_id
        JOIN meeting_participants receiver ON receiver.id = settlement_request.receiver_meeting_participant_id
        JOIN group_members sender_member ON sender_member.id = sender.group_member_id
        JOIN group_members receiver_member ON receiver_member.id = receiver.group_member_id
        WHERE settlement_request.status = :status
          AND (sender_member.user_id = :userId OR receiver_member.user_id = :userId)
        LIMIT 1
        FOR UPDATE
        """,
        nativeQuery = true,
    )
    fun findFirstIdByUserIdAndStatusForUpdate(
        @Param("userId") userId: Long,
        @Param("status") status: String,
    ): Long?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT settlementRequest
        FROM SettlementRequest settlementRequest
        JOIN FETCH settlementRequest.meeting
        JOIN FETCH settlementRequest.sender sender
        JOIN FETCH sender.groupMember senderMember
        JOIN FETCH senderMember.user
        JOIN FETCH settlementRequest.receiver receiver
        JOIN FETCH receiver.groupMember receiverMember
        JOIN FETCH receiverMember.user
        WHERE settlementRequest.id = :settlementRequestId
        """,
    )
    fun findByIdForUpdate(
        @Param("settlementRequestId") settlementRequestId: Long,
    ): SettlementRequest?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT settlementRequest
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.meeting.id = :meetingId
          AND settlementRequest.currency = :currency
          AND settlementRequest.status = :pendingStatus
          AND (
              (settlementRequest.sender.id = :participantId AND settlementRequest.receiver.id = :otherParticipantId)
              OR (settlementRequest.sender.id = :otherParticipantId AND settlementRequest.receiver.id = :participantId)
          )
        """,
    )
    fun findPendingBetweenForUpdate(
        @Param("meetingId") meetingId: Long,
        @Param("currency") currency: BillCurrency,
        @Param("participantId") participantId: Long,
        @Param("otherParticipantId") otherParticipantId: Long,
        @Param("pendingStatus") pendingStatus: SettlementRequestStatus,
    ): SettlementRequest?
}
