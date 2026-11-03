package org.com.belog.notification.domain

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
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.user.domain.User
import java.time.Instant

const val NOTIFICATION_DEDUPLICATION_KEY_UNIQUE_CONSTRAINT_NAME = "uk_notifications_deduplication_key"
private const val NOTIFICATION_TYPE_MAX_LENGTH = 50
private const val NOTIFICATION_MESSAGE_MAX_LENGTH = 200
private const val NOTIFICATION_DEDUPLICATION_KEY_MAX_LENGTH = 150

@Entity
@Table(
    name = "notifications",
    uniqueConstraints = [
        UniqueConstraint(
            name = NOTIFICATION_DEDUPLICATION_KEY_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["deduplication_key"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_notifications_recipient_id",
            columnList = "recipient_user_id, id",
        ),
        Index(
            name = "idx_notifications_recipient_read_at",
            columnList = "recipient_user_id, read_at",
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_notifications_message",
            constraint = "CHAR_LENGTH(TRIM(message)) BETWEEN 1 AND $NOTIFICATION_MESSAGE_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_notifications_deduplication_key",
            constraint =
                "CHAR_LENGTH(TRIM(deduplication_key)) " +
                    "BETWEEN 1 AND $NOTIFICATION_DEDUPLICATION_KEY_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_notifications_target",
            constraint =
                "(type = 'GROUP_DELETED' AND target_id IS NULL) OR " +
                    "(type <> 'GROUP_DELETED' AND target_id IS NOT NULL AND target_id > 0)",
        ),
    ],
)
class Notification protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "recipient_user_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_notifications_recipient_user_id"),
    )
    val recipient: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "actor_user_id",
        foreignKey = ForeignKey(name = "fk_notifications_actor_user_id"),
    )
    val actor: User?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = NOTIFICATION_TYPE_MAX_LENGTH)
    val type: NotificationType,
    @Column(nullable = false, length = NOTIFICATION_MESSAGE_MAX_LENGTH)
    val message: String,
    @Column(name = "target_id")
    val targetId: Long?,
    @Column(name = "deduplication_key", nullable = false, length = NOTIFICATION_DEDUPLICATION_KEY_MAX_LENGTH)
    val deduplicationKey: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "read_at")
    var readAt: Instant? = null
        protected set

    val isRead: Boolean
        get() = readAt != null

    fun markAsRead(readAt: Instant): Boolean {
        if (isRead) {
            return false
        }

        this.readAt = readAt
        return true
    }

    companion object {
        const val TYPE_MAX_LENGTH = NOTIFICATION_TYPE_MAX_LENGTH
        const val MESSAGE_MAX_LENGTH = NOTIFICATION_MESSAGE_MAX_LENGTH
        const val DEDUPLICATION_KEY_MAX_LENGTH = NOTIFICATION_DEDUPLICATION_KEY_MAX_LENGTH

        fun create(
            recipient: User,
            actor: User?,
            type: NotificationType,
            message: String,
            targetId: Long?,
            deduplicationKey: String,
        ): Notification {
            val normalizedMessage = message.trim()
            val normalizedDeduplicationKey = deduplicationKey.trim()

            require(normalizedMessage.length in 1..MESSAGE_MAX_LENGTH) {
                "알림 문구는 비어 있을 수 없으며 ${MESSAGE_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            require(normalizedDeduplicationKey.length in 1..DEDUPLICATION_KEY_MAX_LENGTH) {
                "알림 중복 방지 키는 비어 있을 수 없으며 ${DEDUPLICATION_KEY_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            require(
                (!type.targetType.requiresId && targetId == null) ||
                    (type.targetType.requiresId && targetId != null && targetId > 0),
            ) {
                "그룹 삭제 알림에는 대상 ID가 없어야 하며, 그 외 알림에는 양수인 대상 ID가 필요합니다."
            }

            return Notification(
                recipient = recipient,
                actor = actor,
                type = type,
                message = normalizedMessage,
                targetId = targetId,
                deduplicationKey = normalizedDeduplicationKey,
            )
        }
    }
}
