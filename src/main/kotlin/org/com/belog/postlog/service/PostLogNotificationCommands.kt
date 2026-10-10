package org.com.belog.postlog.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand
import java.time.LocalDate

internal object PostLogNotificationCommands {
    private const val PHOTOS_REGISTERED_MESSAGE_SUFFIX = " 님이 사진을 등록했어요"
    private const val REVIEW_CREATED_MESSAGE_SUFFIX = " 님이 후기를 남겼어요"
    private const val POST_LOG_TODAY_REMINDER_MESSAGE = "오늘 모임은 어땠나요? 사진을 남겨보세요"

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

    fun reviewCreated(
        meetingId: Long,
        ticketId: Long,
        recipientUserId: Long,
        creatorUserId: Long,
        creatorNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = creatorUserId,
            type = NotificationType.POST_LOG_REVIEW_CREATED,
            message = creatorNickname + REVIEW_CREATED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.POST_LOG_REVIEW_CREATED}:$ticketId:$recipientUserId",
        )

    fun postLogTodayReminder(
        meetingId: Long,
        endDate: LocalDate,
        recipientUserId: Long,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = null,
            type = NotificationType.POST_LOG_TODAY_REMINDER,
            message = POST_LOG_TODAY_REMINDER_MESSAGE,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.POST_LOG_TODAY_REMINDER}:$meetingId:$endDate:$recipientUserId",
        )
}
