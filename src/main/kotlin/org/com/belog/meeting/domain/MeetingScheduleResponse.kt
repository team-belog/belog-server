package org.com.belog.meeting.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import java.time.Instant

const val MEETING_SCHEDULE_RESPONSE_UNIQUE_CONSTRAINT_NAME =
    "uk_meeting_schedule_responses_meeting_participant"

@Entity
@Table(
    name = "meeting_schedule_responses",
    uniqueConstraints = [
        UniqueConstraint(
            name = MEETING_SCHEDULE_RESPONSE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["meeting_id", "meeting_participant_id"],
        ),
    ],
)
class MeetingScheduleResponse protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_schedule_responses_meeting_id"),
    )
    val meeting: Meeting,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_participant_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_schedule_responses_participant_id"),
    )
    val participant: MeetingParticipant,
    @Column(name = "responded_at", nullable = false, updatable = false)
    val respondedAt: Instant,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            meeting: Meeting,
            participant: MeetingParticipant,
            respondedAt: Instant,
        ): MeetingScheduleResponse {
            require(participant.belongsTo(meeting)) {
                "응답자는 해당 만남의 참여자여야 합니다."
            }
            require(!meeting.isCreatedBy(participant.groupMember)) {
                "만남 생성자는 후보 일정에 응답할 수 없습니다."
            }
            require(meeting.scheduleType == MeetingScheduleType.POLL) {
                "후보 일정 응답은 일정 조율 방식의 만남에만 등록할 수 있습니다."
            }
            require(meeting.status == MeetingStatus.SCHEDULING) {
                "일정 조율 중인 만남에만 후보 일정 응답을 등록할 수 있습니다."
            }

            return MeetingScheduleResponse(
                meeting = meeting,
                participant = participant,
                respondedAt = respondedAt,
            )
        }
    }
}

internal fun MeetingParticipant.belongsTo(meeting: Meeting): Boolean {
    if (this.meeting === meeting) {
        return true
    }

    val participantMeetingId = this.meeting.id
    val meetingId = meeting.id
    return participantMeetingId != null && meetingId != null && participantMeetingId == meetingId
}
