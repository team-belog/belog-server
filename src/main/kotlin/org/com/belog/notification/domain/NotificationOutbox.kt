package org.com.belog.notification.domain

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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import java.time.Instant

const val NOTIFICATION_OUTBOX_NOTIFICATION_UNIQUE_CONSTRAINT_NAME = "uk_notification_outbox_notification_id"
const val NOTIFICATION_OUTBOX_EVENT_KEY_UNIQUE_CONSTRAINT_NAME = "uk_notification_outbox_event_key"

@Entity
@Table(
    name = "notification_outbox",
    uniqueConstraints = [
        UniqueConstraint(
            name = NOTIFICATION_OUTBOX_NOTIFICATION_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["notification_id"],
        ),
        UniqueConstraint(
            name = NOTIFICATION_OUTBOX_EVENT_KEY_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["event_key"],
        ),
    ],
    indexes = [
        Index(name = "idx_notification_outbox_status_next_attempt", columnList = "status, next_attempt_at, id"),
    ],
)
class NotificationOutbox protected constructor(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "notification_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_notification_outbox_notification_id"),
    )
    val notification: Notification,
    @Column(name = "event_key", nullable = false, length = EVENT_KEY_MAX_LENGTH)
    val eventKey: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: NotificationOutboxStatus = NotificationOutboxStatus.PENDING
        protected set

    @Column(name = "attempt_count", nullable = false)
    var attemptCount: Int = 0
        protected set

    @Column(name = "next_attempt_at")
    var nextAttemptAt: Instant? = null
        protected set

    @Column(name = "processing_started_at")
    var processingStartedAt: Instant? = null
        protected set

    @Column(name = "completed_at")
    var completedAt: Instant? = null
        protected set

    @Column(name = "last_failure_reason", length = FAILURE_REASON_MAX_LENGTH)
    var lastFailureReason: String? = null
        protected set

    fun markProcessing(startedAt: Instant) {
        check(status == NotificationOutboxStatus.PENDING) { "발송 대기 상태의 Outbox만 처리를 시작할 수 있습니다." }
        status = NotificationOutboxStatus.PROCESSING
        attemptCount += 1
        processingStartedAt = startedAt
    }

    fun markSent(completedAt: Instant) {
        check(status == NotificationOutboxStatus.PROCESSING) { "처리 중 상태의 Outbox만 발송 성공으로 기록할 수 있습니다." }
        status = NotificationOutboxStatus.SENT
        this.completedAt = completedAt
    }

    fun markSkipped(completedAt: Instant) {
        check(status == NotificationOutboxStatus.PENDING || status == NotificationOutboxStatus.PROCESSING) {
            "발송 대기 또는 처리 중 상태의 Outbox만 발송 생략으로 기록할 수 있습니다."
        }
        status = NotificationOutboxStatus.SKIPPED
        this.completedAt = completedAt
    }

    fun markFailedForRetry(
        nextAttemptAt: Instant,
        failureReason: String,
    ) {
        check(status == NotificationOutboxStatus.PROCESSING) { "처리 중 상태의 Outbox만 재시도 대상으로 되돌릴 수 있습니다." }
        status = NotificationOutboxStatus.PENDING
        this.nextAttemptAt = nextAttemptAt
        this.lastFailureReason = normalizeFailureReason(failureReason)
    }

    fun markFinalFailure(
        completedAt: Instant,
        failureReason: String,
    ) {
        check(status == NotificationOutboxStatus.PROCESSING) { "처리 중 상태의 Outbox만 최종 실패로 기록할 수 있습니다." }
        status = NotificationOutboxStatus.FAILED
        this.completedAt = completedAt
        this.lastFailureReason = normalizeFailureReason(failureReason)
    }

    companion object {
        const val EVENT_KEY_MAX_LENGTH = 150
        const val FAILURE_REASON_MAX_LENGTH = 500

        fun create(
            notification: Notification,
            eventKey: String,
        ): NotificationOutbox {
            val normalizedEventKey = eventKey.trim()
            require(normalizedEventKey.isNotEmpty() && normalizedEventKey.length <= EVENT_KEY_MAX_LENGTH) {
                "이벤트 키는 비어 있을 수 없으며 ${EVENT_KEY_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            return NotificationOutbox(notification = notification, eventKey = normalizedEventKey)
        }

        private fun normalizeFailureReason(value: String): String = value.take(FAILURE_REASON_MAX_LENGTH)
    }
}
