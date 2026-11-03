package org.com.belog.prelog.worker

import org.com.belog.global.time.BUSINESS_TIME_ZONE
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.worker.NotificationReminderRunner
import org.com.belog.prelog.service.PreLogReminderService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock

@Component
@Profile("!test")
class PreLogReminderWorker(
    private val preLogReminderService: PreLogReminderService,
    private val reminderRunner: NotificationReminderRunner,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${notification.reminder.morning-cron}", zone = BUSINESS_TIME_ZONE)
    fun remindPreLogsBeforeMeetingStart() {
        val currentDate = clock.currentBusinessDate()
        reminderRunner.run(
            reminderType = NotificationType.PRE_LOG_D2_REMINDER,
            findTargetIds = { cursor, size -> preLogReminderService.findMeetingIdsStartingInTwoDays(currentDate, cursor, size) },
            remind = { targetId -> preLogReminderService.remindPreLogBeforeMeetingStart(targetId, currentDate) },
        )
    }
}
