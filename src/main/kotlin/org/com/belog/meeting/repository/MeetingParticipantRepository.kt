package org.com.belog.meeting.repository

import jakarta.persistence.LockModeType
import org.com.belog.meeting.domain.MeetingParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MeetingParticipantRepository : JpaRepository<MeetingParticipant, Long> {
    fun countByMeetingId(meetingId: Long): Long

    fun existsByMeetingIdAndGroupMemberId(
        meetingId: Long,
        groupMemberId: Long,
    ): Boolean

    fun findByMeetingIdAndGroupMemberUserId(
        meetingId: Long,
        userId: Long,
    ): MeetingParticipant?

    @Query(
        """
        SELECT participant
        FROM MeetingParticipant participant
        JOIN FETCH participant.meeting meeting
        JOIN FETCH meeting.createdBy
        JOIN FETCH participant.groupMember groupMember
        JOIN FETCH groupMember.user
        WHERE meeting.id = :meetingId
        ORDER BY participant.id ASC
        """,
    )
    fun findAllWithMemberAndUserByMeetingId(
        @Param("meetingId") meetingId: Long,
    ): List<MeetingParticipant>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT participant
        FROM MeetingParticipant participant
        WHERE participant.meeting.id = :meetingId
          AND participant.groupMember.user.id = :userId
        """,
    )
    fun findByMeetingIdAndUserIdForUpdate(
        @Param("meetingId") meetingId: Long,
        @Param("userId") userId: Long,
    ): MeetingParticipant?
}
