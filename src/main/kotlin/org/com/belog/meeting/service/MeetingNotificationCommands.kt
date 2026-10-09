package org.com.belog.meeting.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object MeetingNotificationCommands {
    private const val DATE_POLL_STARTED_MESSAGE = "새로운 일정 조율이 시작됐어요, 되는 날짜를 체크해주세요"

    fun datePollStarted(
        meetingId: Long,
        recipientUserId: Long,
        creatorUserId: Long,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = creatorUserId,
            type = NotificationType.DATE_POLL_STARTED,
            message = DATE_POLL_STARTED_MESSAGE,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.DATE_POLL_STARTED}:$meetingId:$recipientUserId",
        )
}
