package org.com.belog.group.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object GroupNotificationCommands {
    private const val GROUP_MEMBER_JOINED_MESSAGE_SUFFIX = " 님이 그룹에 참여했어요"
    private const val GROUP_DELETED_MESSAGE_SUFFIX = " 그룹이 삭제됐어요"
    private const val GROUP_INACTIVE_60_DAYS_MESSAGE = "마지막 만남 이후 60일, 슬슬 다시 만나볼 때 아닌가요?"

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

    fun groupDeleted(
        groupId: Long,
        groupName: String,
        recipientUserId: Long,
        ownerUserId: Long,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = ownerUserId,
            type = NotificationType.GROUP_DELETED,
            message = groupName + GROUP_DELETED_MESSAGE_SUFFIX,
            targetId = null,
            deduplicationKey = "${NotificationType.GROUP_DELETED}:$groupId:$recipientUserId",
        )

    fun groupInactive60Days(
        groupId: Long,
        lastMeetingId: Long,
        recipientUserId: Long,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = null,
            type = NotificationType.GROUP_INACTIVE_60_DAYS,
            message = GROUP_INACTIVE_60_DAYS_MESSAGE,
            targetId = groupId,
            deduplicationKey = "${NotificationType.GROUP_INACTIVE_60_DAYS}:$groupId:$lastMeetingId:$recipientUserId",
        )
}
