package org.com.belog.notification.repository

import org.com.belog.notification.domain.NotificationDevice
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationDeviceRepository : JpaRepository<NotificationDevice, Long> {
    fun findByUserIdAndDeviceId(
        userId: Long,
        deviceId: String,
    ): NotificationDevice?

    fun findByFcmTokenAndDeactivatedAtIsNull(fcmToken: String): NotificationDevice?

    fun findAllByUserIdAndDeactivatedAtIsNull(userId: Long): List<NotificationDevice>
}
