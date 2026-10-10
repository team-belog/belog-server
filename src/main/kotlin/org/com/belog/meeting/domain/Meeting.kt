package org.com.belog.meeting.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.com.belog.global.domain.BaseEntity
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

private const val MEETING_NAME_MIN_LENGTH = 1
private const val MEETING_NAME_MAX_LENGTH = 15
private const val MEETING_LOCATION_MAX_LENGTH = 20

@Entity
@Table(
    name = "meetings",
    check = [
        CheckConstraint(
            name = "chk_meetings_name",
            constraint = "CHAR_LENGTH(TRIM(name)) BETWEEN $MEETING_NAME_MIN_LENGTH AND $MEETING_NAME_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_meetings_location",
            constraint =
                "location IS NULL OR " +
                    "CHAR_LENGTH(TRIM(location)) BETWEEN 1 AND $MEETING_LOCATION_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_meetings_schedule_state",
            constraint =
                "(" +
                    "schedule_type = 'FIXED' AND status = 'CONFIRMED' " +
                    "AND start_date IS NOT NULL AND end_date IS NOT NULL AND confirmed_at IS NOT NULL" +
                    ") OR (" +
                    "schedule_type = 'POLL' AND (" +
                    "(status = 'SCHEDULING' AND start_date IS NULL AND end_date IS NULL AND confirmed_at IS NULL) OR " +
                    "(status = 'CONFIRMED' AND start_date IS NOT NULL AND end_date IS NOT NULL " +
                    "AND confirmed_at IS NOT NULL)" +
                    ")" +
                    ")",
        ),
        CheckConstraint(
            name = "chk_meetings_date_range",
            constraint = "start_date IS NULL OR end_date >= start_date",
        ),
    ],
    indexes = [
        Index(
            name = "idx_meetings_start_date_id",
            columnList = "start_date, id",
        ),
        Index(
            name = "idx_meetings_end_date_id",
            columnList = "end_date, id",
        ),
        Index(
            name = "idx_meetings_group_end_date",
            columnList = "group_id, end_date",
        ),
    ],
)
class Meeting protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "group_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meetings_group_id"),
    )
    val group: Group,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meetings_created_by_group_member_id"),
    )
    val createdBy: GroupMember,
    owner: GroupMember,
    name: String,
    location: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    val scheduleType: MeetingScheduleType,
    status: MeetingStatus,
    startDate: LocalDate?,
    endDate: LocalDate?,
    confirmedAt: Instant?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "owner_group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meetings_owner_group_member_id"),
    )
    var owner: GroupMember = owner
        protected set

    @Column(nullable = false, length = MEETING_NAME_MAX_LENGTH)
    var name: String = name
        protected set

    @Column(length = MEETING_LOCATION_MAX_LENGTH)
    var location: String? = location
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MeetingStatus = status
        protected set

    @Column(name = "start_date")
    var startDate: LocalDate? = startDate
        protected set

    @Column(name = "end_date")
    var endDate: LocalDate? = endDate
        protected set

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = confirmedAt
        protected set

    @Column(name = "last_date_poll_reminded_at")
    var lastDatePollRemindedAt: Instant? = null
        protected set

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null
        protected set

    fun isCreatedBy(groupMember: GroupMember): Boolean {
        if (createdBy === groupMember) {
            return true
        }

        val creatorId = createdBy.id
        val groupMemberId = groupMember.id
        return creatorId != null && groupMemberId != null && creatorId == groupMemberId
    }

    fun isOwnedBy(groupMember: GroupMember): Boolean {
        if (owner === groupMember) {
            return true
        }

        val ownerId = owner.id
        val groupMemberId = groupMember.id
        return ownerId != null && groupMemberId != null && ownerId == groupMemberId
    }

    fun delegateOwnerTo(newOwner: GroupMember) {
        require(newOwner.belongsTo(group)) { "같은 그룹의 멤버에게만 여정 방장을 위임할 수 있습니다." }
        require(newOwner.isActive) { "탈퇴한 멤버에게는 여정 방장을 위임할 수 없습니다." }

        owner = newOwner
    }

    fun isEnded(currentDate: LocalDate): Boolean = endDate?.isBefore(currentDate) == true

    fun updateDetails(
        name: String,
        location: String?,
    ): Boolean {
        val normalizedName = normalizeName(name)
        val normalizedLocation = normalizeLocation(location)

        if (this.name == normalizedName && this.location == normalizedLocation) {
            return false
        }

        this.name = normalizedName
        this.location = normalizedLocation
        return true
    }

    fun isDatePollReminderCooldownElapsed(now: Instant): Boolean {
        val lastDatePollRemindedAt = lastDatePollRemindedAt ?: return true
        return !now.isBefore(lastDatePollRemindedAt.plus(DATE_POLL_REMINDER_COOLDOWN))
    }

    fun remindDatePoll(remindedAt: Instant) {
        check(status == MeetingStatus.SCHEDULING) { "일정 조율 중인 만남에만 리마인드할 수 있습니다." }
        check(isDatePollReminderCooldownElapsed(remindedAt)) { "일정 조율 리마인드 대기 시간이 지나지 않았습니다." }

        lastDatePollRemindedAt = remindedAt
    }

    fun delete(deletedAt: Instant) {
        require(this.deletedAt == null) { "이미 삭제된 만남입니다." }
        this.deletedAt = deletedAt
    }

    fun confirmDate(
        candidateDateRange: MeetingCandidateDateRange,
        confirmedAt: Instant,
        currentDate: LocalDate,
    ): Boolean {
        require(scheduleType == MeetingScheduleType.POLL) {
            "일정 조율 방식의 만남만 후보 일정을 확정할 수 있습니다."
        }
        require(candidateDateRange.belongsTo(this)) {
            "해당 만남에 등록된 후보 일정만 확정할 수 있습니다."
        }

        if (
            status == MeetingStatus.CONFIRMED &&
            startDate == candidateDateRange.startDate &&
            endDate == candidateDateRange.endDate
        ) {
            return false
        }

        require(status == MeetingStatus.SCHEDULING) {
            "일정 조율 중인 만남만 후보 일정을 확정할 수 있습니다."
        }
        require(!candidateDateRange.startDate.isBefore(currentDate)) {
            "과거 날짜를 만남 일정으로 확정할 수 없습니다."
        }

        status = MeetingStatus.CONFIRMED
        startDate = candidateDateRange.startDate
        endDate = candidateDateRange.endDate
        this.confirmedAt = confirmedAt
        return true
    }

    fun changeConfirmedDate(
        dateRange: MeetingDateRange,
        currentDate: LocalDate,
    ): Boolean {
        require(status == MeetingStatus.CONFIRMED) {
            "확정된 만남의 일정만 변경할 수 있습니다."
        }

        if (startDate == dateRange.startDate && endDate == dateRange.endDate) {
            return false
        }

        require(!isEnded(currentDate)) {
            "종료된 만남의 일정은 변경할 수 없습니다."
        }
        require(!dateRange.endDate.isBefore(currentDate)) {
            "종료된 일정으로 변경할 수 없습니다."
        }

        startDate = dateRange.startDate
        endDate = dateRange.endDate
        return true
    }

    companion object {
        const val NAME_MIN_LENGTH = MEETING_NAME_MIN_LENGTH
        const val NAME_MAX_LENGTH = MEETING_NAME_MAX_LENGTH
        const val LOCATION_MAX_LENGTH = MEETING_LOCATION_MAX_LENGTH
        private val DATE_POLL_REMINDER_COOLDOWN: Duration = Duration.ofMinutes(1)

        fun createFixed(
            group: Group,
            creator: GroupMember,
            name: String,
            location: String?,
            dateRange: MeetingDateRange,
            confirmedAt: Instant,
            currentDate: LocalDate,
        ): Meeting {
            require(creator.belongsTo(group)) { "만남 생성자는 해당 그룹의 멤버여야 합니다." }

            val normalizedName = normalizeName(name)
            val normalizedLocation = normalizeLocation(location)
            require(!dateRange.startDate.isBefore(currentDate)) { "과거 날짜로 만남을 생성할 수 없습니다." }

            return Meeting(
                group = group,
                createdBy = creator,
                owner = creator,
                name = normalizedName,
                location = normalizedLocation,
                scheduleType = MeetingScheduleType.FIXED,
                status = MeetingStatus.CONFIRMED,
                startDate = dateRange.startDate,
                endDate = dateRange.endDate,
                confirmedAt = confirmedAt,
            )
        }

        fun createPoll(
            group: Group,
            creator: GroupMember,
            name: String,
            location: String?,
        ): Meeting {
            require(creator.belongsTo(group)) { "만남 생성자는 해당 그룹의 멤버여야 합니다." }

            val normalizedName = normalizeName(name)
            val normalizedLocation = normalizeLocation(location)

            return Meeting(
                group = group,
                createdBy = creator,
                owner = creator,
                name = normalizedName,
                location = normalizedLocation,
                scheduleType = MeetingScheduleType.POLL,
                status = MeetingStatus.SCHEDULING,
                startDate = null,
                endDate = null,
                confirmedAt = null,
            )
        }

        private fun normalizeName(name: String): String {
            val normalizedName = name.trim()
            require(normalizedName.length in NAME_MIN_LENGTH..NAME_MAX_LENGTH) {
                "만남명은 ${NAME_MIN_LENGTH}자 이상 ${NAME_MAX_LENGTH}자 이하여야 합니다."
            }
            return normalizedName
        }

        private fun normalizeLocation(location: String?): String? {
            val normalizedLocation = location?.trim()?.takeIf(String::isNotEmpty)
            require(normalizedLocation == null || normalizedLocation.length <= LOCATION_MAX_LENGTH) {
                "만남 장소는 ${LOCATION_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            return normalizedLocation
        }
    }
}
