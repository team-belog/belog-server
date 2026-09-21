package org.com.belog.meeting.domain

import jakarta.persistence.CheckConstraint
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
import java.time.LocalDate

const val MEETING_CANDIDATE_DATE_RANGE_UNIQUE_CONSTRAINT_NAME =
    "uk_meeting_candidate_date_ranges_meeting_dates"

@Entity
@Table(
    name = "meeting_candidate_date_ranges",
    uniqueConstraints = [
        UniqueConstraint(
            name = MEETING_CANDIDATE_DATE_RANGE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["meeting_id", "start_date", "end_date"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_meeting_candidate_date_ranges_date_range",
            constraint = "end_date >= start_date",
        ),
    ],
)
class MeetingCandidateDateRange protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_candidate_date_ranges_meeting_id"),
    )
    val meeting: Meeting,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            meeting: Meeting,
            startDate: LocalDate,
            endDate: LocalDate,
            currentDate: LocalDate,
        ): MeetingCandidateDateRange {
            require(meeting.scheduleType == MeetingScheduleType.POLL) {
                "후보 일정 범위는 일정 조율 방식의 만남에만 등록할 수 있습니다."
            }
            require(meeting.status == MeetingStatus.SCHEDULING) {
                "일정 조율 중인 만남에만 후보 일정 범위를 등록할 수 있습니다."
            }
            require(!startDate.isBefore(currentDate)) {
                "과거 날짜를 후보 일정 범위로 등록할 수 없습니다."
            }
            require(!endDate.isBefore(startDate)) {
                "종료일은 시작일보다 빠를 수 없습니다."
            }

            return MeetingCandidateDateRange(
                meeting = meeting,
                startDate = startDate,
                endDate = endDate,
            )
        }
    }
}
