package org.com.belog.postlog.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object PostLogNotificationCommands {
    private const val PHOTOS_REGISTERED_MESSAGE_SUFFIX = " 님이 사진을 등록했어요"

    fun photosRegistered(
        meetingId: Long,
        firstPhotoId: Long,
        recipientUserId: Long,
        uploaderUserId: Long,
        uploaderNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = uploaderUserId,
            type = NotificationType.POST_LOG_PHOTOS_REGISTERED,
            message = uploaderNickname + PHOTOS_REGISTERED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.POST_LOG_PHOTOS_REGISTERED}:$firstPhotoId:$recipientUserId",
        )
}
