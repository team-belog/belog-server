package org.com.belog.notification.service

import org.com.belog.global.error.BusinessException
import org.com.belog.notification.code.NotificationErrorCode
import org.com.belog.notification.domain.NotificationDevice
import org.com.belog.notification.repository.NotificationDeviceRepository
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class NotificationDeviceService(
    private val notificationDeviceRepository: NotificationDeviceRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    @Transactional
    fun registerDevice(
        userId: Long,
        deviceId: String,
        fcmToken: String,
    ) {
        val normalizedFcmToken = fcmToken.trim()
        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        deactivateConflictingDevice(user = user, deviceId = deviceId, fcmToken = normalizedFcmToken)

        val existingDevice = notificationDeviceRepository.findByUserIdAndDeviceId(userId, deviceId)
        if (existingDevice != null) {
            existingDevice.updateToken(normalizedFcmToken)
            return
        }

        notificationDeviceRepository.save(
            NotificationDevice.register(user = user, deviceId = deviceId, fcmToken = normalizedFcmToken),
        )
    }

    @Transactional
    fun unregisterDevice(
        userId: Long,
        deviceId: String,
    ) {
        val device =
            notificationDeviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                ?: throw BusinessException(NotificationErrorCode.NOTIFICATION_DEVICE_NOT_FOUND)

        device.deactivate(Instant.now(clock))
    }

    private fun deactivateConflictingDevice(
        user: User,
        deviceId: String,
        fcmToken: String,
    ) {
        val conflictingDevice =
            notificationDeviceRepository.findByFcmTokenAndDeactivatedAtIsNull(fcmToken) ?: return
        val isSameRegistration = conflictingDevice.belongsTo(user) && conflictingDevice.deviceId == deviceId

        if (!isSameRegistration) {
            conflictingDevice.deactivate(Instant.now(clock))
        }
    }
}
