package org.com.belog.notification.service

import org.com.belog.global.error.BusinessException
import org.com.belog.notification.code.NotificationErrorCode
import org.com.belog.notification.domain.Notification
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.notification.service.command.CreateNotificationCommand
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getPushNotificationEnabled(userId: Long): Boolean {
        val user =
            userRepository.findById(userId).orElseThrow {
                BusinessException(UserErrorCode.USER_NOT_FOUND)
            }

        return user.pushNotificationEnabled
    }

    @Transactional
    fun updatePushNotificationEnabled(
        userId: Long,
        enabled: Boolean,
    ) {
        val user =
            userRepository.findByIdForUpdate(userId)
                ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

        user.updatePushNotificationEnabled(enabled)
    }

    @Transactional
    fun create(command: CreateNotificationCommand) {
        createAll(listOf(command))
    }

    @Transactional
    fun createAll(commands: List<CreateNotificationCommand>) {
        if (commands.isEmpty()) {
            return
        }

        val usersById = findUsersById(commands)
        val activeCommands = commands.filter { command -> usersById.getUser(command.recipientUserId).isActive }

        activeCommands.forEach { command ->
            notificationRepository.saveIfAbsent(
                Notification.create(
                    recipient = usersById.getUser(command.recipientUserId),
                    actor = command.actorUserId?.let { actorUserId -> usersById.getUser(actorUserId) },
                    type = command.type,
                    message = command.message,
                    targetId = command.targetId,
                    deduplicationKey = command.deduplicationKey,
                ),
            )
        }
    }

    @Transactional
    fun markAsRead(
        notificationId: Long,
        userId: Long,
    ) {
        val notification =
            notificationRepository.findByIdAndRecipientUserIdForUpdate(
                notificationId = notificationId,
                recipientUserId = userId,
            ) ?: throw BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND)

        notification.markAsRead(Instant.now(clock))
    }

    private fun findUsersById(commands: List<CreateNotificationCommand>): Map<Long, User> {
        val userIds =
            commands
                .flatMap { command -> listOfNotNull(command.recipientUserId, command.actorUserId) }
                .toSet()

        return userRepository
            .findAllById(userIds)
            .associateBy { user -> requireNotNull(user.id) }
            .also { usersById ->
                if (usersById.size != userIds.size) {
                    throw BusinessException(UserErrorCode.USER_NOT_FOUND)
                }
            }
    }

    private fun Map<Long, User>.getUser(userId: Long): User = this[userId] ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)
}
