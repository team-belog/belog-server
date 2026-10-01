package org.com.belog.notification.service

import org.com.belog.global.error.BusinessException
import org.com.belog.notification.domain.Notification
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.notification.service.command.CreateNotificationCommand
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository,
) {
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

        commands.forEach { command ->
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
