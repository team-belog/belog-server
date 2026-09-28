package org.com.belog.postlog.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.meeting.domain.Meeting

const val POST_LOG_MEETING_UNIQUE_CONSTRAINT_NAME = "uk_post_logs_meeting_id"

@Entity
@Table(
    name = "post_logs",
    uniqueConstraints = [
        UniqueConstraint(
            name = POST_LOG_MEETING_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["meeting_id"],
        ),
    ],
)
class PostLog protected constructor(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_logs_meeting_id"),
    )
    val meeting: Meeting,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(meeting: Meeting): PostLog = PostLog(meeting = meeting)
    }
}
