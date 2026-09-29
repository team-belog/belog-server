package org.com.belog.billlog.repository

import jakarta.persistence.LockModeType
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SettlementRequestRepository : JpaRepository<SettlementRequest, Long> {
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
          AND bill.meeting.id = :meetingId
        """,
    )
    fun findByIdAndMeetingIdForUpdate(
        @Param("settlementRequestId") settlementRequestId: Long,
        @Param("meetingId") meetingId: Long,
    ): SettlementRequest?
}
