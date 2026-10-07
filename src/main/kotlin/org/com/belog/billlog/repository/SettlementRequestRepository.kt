package org.com.belog.billlog.repository

import jakarta.persistence.LockModeType
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
        JOIN FETCH settlementRequest.bill bill
        JOIN FETCH bill.payer payer
        JOIN FETCH payer.groupMember payerMember
        JOIN FETCH payerMember.user
        JOIN FETCH settlementRequest.participant participant
        JOIN FETCH participant.groupMember participantMember
        JOIN FETCH participantMember.user
        WHERE bill.meeting.id = :meetingId
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
        SELECT COUNT(DISTINCT settlementRequest.participant.id)
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.bill.meeting.id = :meetingId
          AND settlementRequest.status = :status
        """,
    )
    fun countDistinctParticipantsByMeetingIdAndStatus(
        @Param("meetingId") meetingId: Long,
        @Param("status") status: SettlementRequestStatus,
    ): Long

    @Query(
        """
        SELECT COUNT(DISTINCT settlementRequest.participant.id)
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.bill.meeting.id = :meetingId
          AND NOT EXISTS (
              SELECT pendingRequest.id
              FROM SettlementRequest pendingRequest
              WHERE pendingRequest.bill.meeting.id = :meetingId
                AND pendingRequest.participant.id = settlementRequest.participant.id
                AND pendingRequest.status = :pendingStatus
          )
        """,
    )
    fun countCompletedParticipantsByMeetingId(
        @Param("meetingId") meetingId: Long,
        @Param("pendingStatus") pendingStatus: SettlementRequestStatus,
    ): Long

    @Query(
        """
        SELECT COUNT(settlementRequest) > 0
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.bill.meeting.id = :meetingId
          AND settlementRequest.status = :status
        """,
    )
    fun existsByMeetingIdAndStatus(
        @Param("meetingId") meetingId: Long,
        @Param("status") status: SettlementRequestStatus,
    ): Boolean

    @Query(
        """
        SELECT COUNT(settlementRequest) > 0
        FROM SettlementRequest settlementRequest
        WHERE settlementRequest.bill.meeting.group.id = :groupId
          AND settlementRequest.status = :status
        """,
    )
    fun existsByGroupIdAndStatus(
        @Param("groupId") groupId: Long,
        @Param("status") status: SettlementRequestStatus,
    ): Boolean

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT settlementRequest
        FROM SettlementRequest settlementRequest
        JOIN FETCH settlementRequest.bill bill
        JOIN FETCH settlementRequest.participant participant
        JOIN FETCH participant.groupMember groupMember
        JOIN FETCH groupMember.user
        WHERE settlementRequest.id = :settlementRequestId
        """,
    )
    fun findByIdForUpdate(
        @Param("settlementRequestId") settlementRequestId: Long,
    ): SettlementRequest?
}
