package org.com.belog.notification.domain

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
import org.com.belog.user.domain.User
import java.time.Instant

const val NOTIFICATION_DEVICE_USER_DEVICE_UNIQUE_CONSTRAINT_NAME = "uk_notification_devices_user_device"
const val NOTIFICATION_DEVICE_ACTIVE_FCM_TOKEN_UNIQUE_CONSTRAINT_NAME = "uk_notification_devices_active_fcm_token"

@Entity
@Table(
    name = "notification_devices",
    uniqueConstraints = [
        UniqueConstraint(
            name = NOTIFICATION_DEVICE_USER_DEVICE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["user_id", "device_id"],
        ),
        UniqueConstraint(
            name = NOTIFICATION_DEVICE_ACTIVE_FCM_TOKEN_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["active_fcm_token"],
        ),
    ],
    indexes = [
        Index(name = "idx_notification_devices_user_deactivated", columnList = "user_id, deactivated_at"),
    ],
)
class NotificationDevice protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_notification_devices_user_id"),
    )
    val user: User,
    @Column(name = "device_id", nullable = false, length = DEVICE_ID_MAX_LENGTH)
    val deviceId: String,
    fcmToken: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "fcm_token", nullable = false, length = FCM_TOKEN_MAX_LENGTH)
    var fcmToken: String = fcmToken
        protected set

    @Column(name = "deactivated_at")
    var deactivatedAt: Instant? = null
        protected set

    @Column(
        name = "active_fcm_token",
        insertable = false,
        updatable = false,
        columnDefinition =
            "VARCHAR($FCM_TOKEN_MAX_LENGTH) GENERATED ALWAYS AS " +
                "(CASE WHEN deactivated_at IS NULL THEN fcm_token ELSE NULL END)",
    )
    private var activeFcmToken: String? = null

    val isActive: Boolean
        get() = deactivatedAt == null

    fun belongsTo(user: User): Boolean {
        if (this.user === user) return true
        val ownerId = this.user.id
        val candidateId = user.id
        return ownerId != null && candidateId != null && ownerId == candidateId
    }

    fun updateToken(fcmToken: String) {
        this.fcmToken = normalizeToken(fcmToken)
        this.deactivatedAt = null
    }

    fun deactivate(deactivatedAt: Instant): Boolean {
        if (!isActive) return false
        this.deactivatedAt = deactivatedAt
        return true
    }

    companion object {
        const val DEVICE_ID_MAX_LENGTH = 255
        const val FCM_TOKEN_MAX_LENGTH = 255

        fun register(
            user: User,
            deviceId: String,
            fcmToken: String,
        ): NotificationDevice =
            NotificationDevice(
                user = user,
                deviceId = normalizeDeviceId(deviceId),
                fcmToken = normalizeToken(fcmToken),
            )

        private fun normalizeDeviceId(value: String): String {
            val normalized = value.trim()
            require(normalized.isNotEmpty() && normalized.length <= DEVICE_ID_MAX_LENGTH) {
                "기기 식별자는 비어 있을 수 없으며 ${DEVICE_ID_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            return normalized
        }

        private fun normalizeToken(value: String): String {
            val normalized = value.trim()
            require(normalized.isNotEmpty() && normalized.length <= FCM_TOKEN_MAX_LENGTH) {
                "FCM 토큰은 비어 있을 수 없으며 ${FCM_TOKEN_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            return normalized
        }
    }
}
