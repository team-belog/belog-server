package org.com.belog.postlog.worker

import org.com.belog.global.time.BUSINESS_TIME_ZONE
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.worker.NotificationReminderRunner
import org.com.belog.postlog.service.PostLogReminderService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock

@Component
@Profile("!test")
class PostLogReminderWorker(
    private val postLogReminderService: PostLogReminderService,
    private val reminderRunner: NotificationReminderRunner,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${notification.reminder.evening-cron}", zone = BUSINESS_TIME_ZONE)
    fun remindPostLogsOnMeetingEndDate() {
        val currentDate = clock.currentBusinessDate()
        reminderRunner.run(
            reminderType = NotificationType.POST_LOG_TODAY_REMINDER,
            findTargetIds = { cursor, size -> postLogReminderService.findMeetingIdsEndingToday(currentDate, cursor, size) },
            remind = { targetId -> postLogReminderService.remindPostLogOnMeetingEndDate(targetId, currentDate) },
        )
    }
}
