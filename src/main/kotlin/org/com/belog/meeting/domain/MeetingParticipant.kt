package org.com.belog.meeting.domain

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

const val MEETING_PARTICIPANT_UNIQUE_CONSTRAINT_NAME = "uk_meeting_participants_meeting_member"

@Entity
@Table(
    name = "meeting_participants",
    uniqueConstraints = [
        UniqueConstraint(
            name = MEETING_PARTICIPANT_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["meeting_id", "group_member_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_meeting_participants_group_member_id_meeting_id",
            columnList = "group_member_id, meeting_id",
        ),
    ],
)
class MeetingParticipant protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_participants_meeting_id"),
    )
    val meeting: Meeting,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_meeting_participants_group_member_id"),
    )
    val groupMember: GroupMember,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            meeting: Meeting,
            groupMember: GroupMember,
        ): MeetingParticipant {
            require(groupMember.belongsTo(meeting.group)) {
                "만남 참여자는 해당 만남과 같은 그룹의 멤버여야 합니다."
            }

            return MeetingParticipant(
                meeting = meeting,
                groupMember = groupMember,
            )
        }
    }
}
