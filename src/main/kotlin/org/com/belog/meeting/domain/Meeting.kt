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
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.com.belog.global.domain.BaseEntity
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
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
                    "AND confirmed_date IS NOT NULL AND confirmed_at IS NOT NULL" +
                    ") OR (" +
                    "schedule_type = 'POLL' AND (" +
                    "(status = 'SCHEDULING' AND confirmed_date IS NULL AND confirmed_at IS NULL) OR " +
                    "(status = 'CONFIRMED' AND confirmed_date IS NOT NULL AND confirmed_at IS NOT NULL)" +
                    ")" +
                    ")",
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
    @Column(nullable = false, length = MEETING_NAME_MAX_LENGTH)
    val name: String,
    @Column(length = MEETING_LOCATION_MAX_LENGTH)
    val location: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    val scheduleType: MeetingScheduleType,
    status: MeetingStatus,
    confirmedDate: LocalDate?,
    confirmedAt: Instant?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MeetingStatus = status
        protected set

    @Column(name = "confirmed_date")
    var confirmedDate: LocalDate? = confirmedDate
        protected set

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = confirmedAt
        protected set

    fun isCreatedBy(groupMember: GroupMember): Boolean {
        if (createdBy === groupMember) {
            return true
        }

        val creatorId = createdBy.id
        val groupMemberId = groupMember.id
        return creatorId != null && groupMemberId != null && creatorId == groupMemberId
    }

    companion object {
        const val NAME_MIN_LENGTH = MEETING_NAME_MIN_LENGTH
        const val NAME_MAX_LENGTH = MEETING_NAME_MAX_LENGTH
        const val LOCATION_MAX_LENGTH = MEETING_LOCATION_MAX_LENGTH

        fun createFixed(
            group: Group,
            creator: GroupMember,
            name: String,
            location: String?,
            confirmedDate: LocalDate,
            confirmedAt: Instant,
            currentDate: LocalDate,
        ): Meeting {
            require(creator.belongsTo(group)) { "만남 생성자는 해당 그룹의 멤버여야 합니다." }

            val normalizedName = name.trim()
            require(normalizedName.length in NAME_MIN_LENGTH..NAME_MAX_LENGTH) {
                "만남명은 ${NAME_MIN_LENGTH}자 이상 ${NAME_MAX_LENGTH}자 이하여야 합니다."
            }

            val normalizedLocation = location?.trim()?.takeIf(String::isNotEmpty)
            require(normalizedLocation == null || normalizedLocation.length <= LOCATION_MAX_LENGTH) {
                "만남 장소는 ${LOCATION_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            require(!confirmedDate.isBefore(currentDate)) { "과거 날짜로 만남을 생성할 수 없습니다." }

            return Meeting(
                group = group,
                createdBy = creator,
                name = normalizedName,
                location = normalizedLocation,
                scheduleType = MeetingScheduleType.FIXED,
                status = MeetingStatus.CONFIRMED,
                confirmedDate = confirmedDate,
                confirmedAt = confirmedAt,
            )
        }
    }
}

internal fun GroupMember.belongsTo(group: Group): Boolean {
    if (this.group === group) {
        return true
    }

    val memberGroupId = this.group.id
    val groupId = group.id
    return memberGroupId != null && groupId != null && memberGroupId == groupId
}
