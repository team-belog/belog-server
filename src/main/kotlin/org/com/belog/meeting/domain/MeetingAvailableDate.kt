package org.com.belog.meeting.domain

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

const val MEETING_AVAILABLE_DATE_UNIQUE_CONSTRAINT_NAME =
    "uk_meeting_available_dates_response_candidate"

@Entity
@Table(
    name = "meeting_available_dates",
    uniqueConstraints = [
        UniqueConstraint(
            name = MEETING_AVAILABLE_DATE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["meeting_schedule_response_id", "meeting_candidate_date_range_id"],
        ),
    ],
)
class MeetingAvailableDate protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_schedule_response_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_available_dates_response_id"),
    )
    val response: MeetingScheduleResponse,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_candidate_date_range_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_available_dates_candidate_id"),
    )
    val candidateDateRange: MeetingCandidateDateRange,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            response: MeetingScheduleResponse,
            candidateDateRange: MeetingCandidateDateRange,
        ): MeetingAvailableDate {
            require(candidateDateRange.belongsTo(response.meeting)) {
                "선택한 후보 일정은 응답 대상 만남에 속해야 합니다."
            }

            return MeetingAvailableDate(
                response = response,
                candidateDateRange = candidateDateRange,
            )
        }
    }
}

internal fun MeetingCandidateDateRange.belongsTo(meeting: Meeting): Boolean {
    if (this.meeting === meeting) {
        return true
    }

    val candidateMeetingId = this.meeting.id
    val meetingId = meeting.id
    return candidateMeetingId != null && meetingId != null && candidateMeetingId == meetingId
}
