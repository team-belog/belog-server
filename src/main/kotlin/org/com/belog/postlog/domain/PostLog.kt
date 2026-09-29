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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.meeting.domain.Meeting
import java.time.Instant

private const val POST_LOG_MEMORY_MIN_LENGTH = 1
private const val POST_LOG_MEMORY_MAX_LENGTH = 80

@Entity
@Table(
    name = "post_logs",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_post_logs_meeting_id",
            columnNames = ["meeting_id"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_post_logs_memory",
            constraint =
                "memory IS NULL OR " +
                    "CHAR_LENGTH(TRIM(memory)) BETWEEN $POST_LOG_MEMORY_MIN_LENGTH AND $POST_LOG_MEMORY_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_post_logs_ticket",
            constraint = "ticket_created_at IS NULL OR memory IS NOT NULL",
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

    @Column(length = MEMORY_MAX_LENGTH)
    var memory: String? = null
        protected set

    @Column(name = "ticket_created_at")
    var ticketCreatedAt: Instant? = null
        protected set

    val isTicketCreated: Boolean
        get() = ticketCreatedAt != null

    fun updateMemory(memory: String) {
        val normalizedMemory = memory.trim()
        require(normalizedMemory.length in MEMORY_MIN_LENGTH..MEMORY_MAX_LENGTH) {
            "추억 문구는 ${MEMORY_MIN_LENGTH}자 이상 ${MEMORY_MAX_LENGTH}자 이하여야 합니다."
        }

        this.memory = normalizedMemory
    }

    fun createTicket(createdAt: Instant): Boolean {
        require(memory != null) { "추억 문구를 작성한 뒤 티켓을 생성할 수 있습니다." }
        if (isTicketCreated) {
            return false
        }

        ticketCreatedAt = createdAt
        return true
    }

    companion object {
        const val MEMORY_MIN_LENGTH = POST_LOG_MEMORY_MIN_LENGTH
        const val MEMORY_MAX_LENGTH = POST_LOG_MEMORY_MAX_LENGTH

        fun create(meeting: Meeting): PostLog = PostLog(meeting = meeting)
    }
}
