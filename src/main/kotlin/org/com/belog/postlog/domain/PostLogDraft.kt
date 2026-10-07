package org.com.belog.postlog.domain

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
import org.com.belog.group.domain.GroupMember
import org.com.belog.meeting.domain.Meeting

@Entity
@Table(
    name = "post_log_drafts",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_log_drafts_meeting_member",
            columnNames = ["meeting_id", "created_by_group_member_id"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_post_log_drafts_memory",
            constraint = PostLogMemory.LENGTH_CHECK_CONSTRAINT,
        ),
    ],
)
class PostLogDraft protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_drafts_meeting_id"),
    )
    val meeting: Meeting,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_drafts_created_by_group_member_id"),
    )
    val createdBy: GroupMember,
    memory: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, length = PostLogMemory.MAX_LENGTH)
    var memory: String = memory
        protected set

    fun updateMemory(memory: String) {
        this.memory = PostLogMemory.normalize(memory)
    }

    companion object {
        fun create(
            meeting: Meeting,
            createdBy: GroupMember,
            memory: String,
        ): PostLogDraft {
            require(createdBy.belongsTo(meeting.group)) {
                "Post-log 작성자는 해당 만남이 속한 그룹의 멤버여야 합니다."
            }

            return PostLogDraft(
                meeting = meeting,
                createdBy = createdBy,
                memory = PostLogMemory.normalize(memory),
            )
        }
    }
}
