package org.com.belog.meeting.service

import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

internal object MeetingNotificationCommands {
    private const val DATE_POLL_STARTED_MESSAGE = "새로운 일정 조율이 시작됐어요, 되는 날짜를 체크해주세요"
    private const val DATE_POLL_RESPONDED_MESSAGE_SUFFIX = " 님이 일정 조율에 응답했어요"
    private const val DATE_POLL_REMINDER_MESSAGE = "아직 응답 안 하셨어요, 되는 날짜를 체크해주세요"
    private const val MEETING_D7_REMINDER_MESSAGE_SUFFIX = " 모임이 일주일 남았어요, 일정과 장소를 확인해보세요"

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

    fun datePollReminder(
        meetingId: Long,
        recipientUserId: Long,
        ownerUserId: Long,
        remindedAt: Instant,
        reminderId: UUID,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = ownerUserId,
            type = NotificationType.DATE_POLL_REMINDER,
            message = DATE_POLL_REMINDER_MESSAGE,
            targetId = meetingId,
            deduplicationKey =
                "${NotificationType.DATE_POLL_REMINDER}:$meetingId:${remindedAt.toEpochMilli()}:$reminderId:$recipientUserId",
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

    fun meetingD7Reminder(
        meetingId: Long,
        meetingName: String,
        startDate: LocalDate,
        recipientUserId: Long,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = null,
            type = NotificationType.MEETING_D7_REMINDER,
            message = meetingName + MEETING_D7_REMINDER_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.MEETING_D7_REMINDER}:$meetingId:$startDate:$recipientUserId",
        )

    private fun formatDateRange(dateRange: MeetingDateRange): String {
        if (dateRange.startDate == dateRange.endDate) {
            return formatDate(dateRange.startDate)
        }
        return "${formatDate(dateRange.startDate)}~${formatDate(dateRange.endDate)}"
    }

    private fun formatDate(date: LocalDate): String = "${date.monthValue}월 ${date.dayOfMonth}일"
}
