package org.com.belog.meeting.worker

import org.com.belog.global.time.BUSINESS_TIME_ZONE
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.meeting.service.MeetingReminderService
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.worker.NotificationReminderRunner
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock

@Component
@Profile("!test")
class MeetingReminderWorker(
    private val meetingReminderService: MeetingReminderService,
    private val reminderRunner: NotificationReminderRunner,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${notification.reminder.morning-cron}", zone = BUSINESS_TIME_ZONE)
    fun remindMeetingsStartingInAWeek() {
        val currentDate = clock.currentBusinessDate()
        reminderRunner.run(
            reminderType = NotificationType.MEETING_D7_REMINDER,
            findTargetIds = { cursor, size -> meetingReminderService.findMeetingIdsStartingInAWeek(currentDate, cursor, size) },
            remind = { targetId -> meetingReminderService.remindMeetingStartingInAWeek(targetId, currentDate) },
        )
    }
}
