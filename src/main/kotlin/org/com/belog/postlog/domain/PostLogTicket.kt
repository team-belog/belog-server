package org.com.belog.postlog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.group.domain.GroupMember
import org.com.belog.meeting.domain.Meeting
import org.com.belog.user.domain.User
import java.time.Instant
import java.time.LocalDate

const val POST_LOG_TICKET_MEETING_CREATOR_UNIQUE_CONSTRAINT_NAME = "uk_post_log_tickets_meeting_creator"

@Entity
@Table(
    name = "post_log_tickets",
    uniqueConstraints = [
        UniqueConstraint(
            name = POST_LOG_TICKET_MEETING_CREATOR_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["source_meeting_id", "creator_group_member_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_post_log_tickets_owner_end_date",
            columnList = "owner_user_id, meeting_end_date, id",
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_post_log_tickets_memory",
            constraint =
                "CHAR_LENGTH(TRIM(memory)) BETWEEN ${PostLog.MEMORY_MIN_LENGTH} AND ${PostLog.MEMORY_MAX_LENGTH}",
        ),
        CheckConstraint(
            name = "chk_post_log_tickets_meeting_name",
            constraint =
                "CHAR_LENGTH(TRIM(meeting_name)) BETWEEN ${Meeting.NAME_MIN_LENGTH} AND ${Meeting.NAME_MAX_LENGTH}",
        ),
    ],
)
class PostLogTicket protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "owner_user_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_tickets_owner_user_id"),
    )
    val owner: User,
    @Column(name = "source_meeting_id", nullable = false)
    val sourceMeetingId: Long,
    @Column(name = "creator_group_member_id", nullable = false)
    val creatorGroupMemberId: Long,
    @Column(name = "creator_nickname", nullable = false, length = User.NICKNAME_MAX_LENGTH)
    val creatorNickname: String,
    @Column(name = "meeting_name", nullable = false, length = Meeting.NAME_MAX_LENGTH)
    val meetingName: String,
    @Column(name = "meeting_location", length = Meeting.LOCATION_MAX_LENGTH)
    val meetingLocation: String?,
    @Column(name = "meeting_start_date")
    val meetingStartDate: LocalDate?,
    @Column(name = "meeting_end_date")
    val meetingEndDate: LocalDate?,
    @Column(nullable = false, length = PostLog.MEMORY_MAX_LENGTH)
    val memory: String,
    @Column(name = "cover_image_object_key", length = COVER_IMAGE_OBJECT_KEY_MAX_LENGTH)
    val coverImageObjectKey: String?,
    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        const val COVER_IMAGE_OBJECT_KEY_MAX_LENGTH = 512

        fun issue(
            meeting: Meeting,
            creator: GroupMember,
            memory: String,
            coverImageObjectKey: String?,
            issuedAt: Instant,
        ): PostLogTicket {
            require(creator.belongsTo(meeting.group)) {
                "티켓 생성자는 해당 만남이 속한 그룹의 멤버여야 합니다."
            }
            val normalizedMemory = memory.trim()
            require(normalizedMemory.length in PostLog.MEMORY_MIN_LENGTH..PostLog.MEMORY_MAX_LENGTH) {
                "추억 문구는 ${PostLog.MEMORY_MIN_LENGTH}자 이상 ${PostLog.MEMORY_MAX_LENGTH}자 이하여야 합니다."
            }

            return PostLogTicket(
                owner = creator.user,
                sourceMeetingId = checkNotNull(meeting.id) { "티켓 대상 만남의 ID가 없습니다." },
                creatorGroupMemberId = checkNotNull(creator.id) { "티켓 생성자의 그룹 멤버 ID가 없습니다." },
                creatorNickname = checkNotNull(creator.user.nickname) { "티켓 생성자의 닉네임이 없습니다." },
                meetingName = meeting.name,
                meetingLocation = meeting.location,
                meetingStartDate = meeting.startDate,
                meetingEndDate = meeting.endDate,
                memory = normalizedMemory,
                coverImageObjectKey = coverImageObjectKey,
                issuedAt = issuedAt,
            )
        }
    }
}
