package org.com.belog.group.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object GroupNotificationCommands {
    private const val GROUP_MEMBER_JOINED_MESSAGE_SUFFIX = " 님이 그룹에 참여했어요"

    fun groupMemberJoined(
        groupId: Long,
        joinedMemberId: Long,
        recipientUserId: Long,
        joinedUserId: Long,
        joinedUserNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = joinedUserId,
            type = NotificationType.GROUP_MEMBER_JOINED,
            message = joinedUserNickname + GROUP_MEMBER_JOINED_MESSAGE_SUFFIX,
            targetId = groupId,
            deduplicationKey = "${NotificationType.GROUP_MEMBER_JOINED}:$joinedMemberId:$recipientUserId",
        )
}
