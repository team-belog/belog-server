package org.com.belog.meeting.service

import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand
import java.time.LocalDate

internal object MeetingNotificationCommands {
    private const val DATE_POLL_STARTED_MESSAGE = "새로운 일정 조율이 시작됐어요, 되는 날짜를 체크해주세요"
    private const val DATE_POLL_RESPONDED_MESSAGE_SUFFIX = " 님이 일정 조율에 응답했어요"

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

    fun datePollResponded(
        meetingId: Long,
        responseParticipantId: Long,
        recipientUserId: Long,
        responderUserId: Long,
        responderNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = responderUserId,
            type = NotificationType.DATE_POLL_RESPONDED,
            message = responderNickname + DATE_POLL_RESPONDED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.DATE_POLL_RESPONDED}:$meetingId:$responseParticipantId",
        )

    fun meetingDateConfirmed(
        meetingId: Long,
        recipientUserId: Long,
        ownerUserId: Long,
        confirmedDateRange: MeetingDateRange,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = ownerUserId,
            type = NotificationType.MEETING_DATE_CONFIRMED,
            message = "일정이 ${formatDateRange(confirmedDateRange)}로 확정됐어요",
            targetId = meetingId,
            deduplicationKey = "${NotificationType.MEETING_DATE_CONFIRMED}:$meetingId:$recipientUserId",
        )

    private fun formatDateRange(dateRange: MeetingDateRange): String {
        if (dateRange.startDate == dateRange.endDate) {
            return formatDate(dateRange.startDate)
        }
        return "${formatDate(dateRange.startDate)}~${formatDate(dateRange.endDate)}"
    }

    private fun formatDate(date: LocalDate): String = "${date.monthValue}월 ${date.dayOfMonth}일"
}
