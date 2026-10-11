package org.com.belog.group.worker

import org.com.belog.global.time.BUSINESS_TIME_ZONE
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.group.service.GroupReminderService
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.worker.NotificationReminderRunner
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock

@Component
@Profile("!test")
class GroupReminderWorker(
    private val groupReminderService: GroupReminderService,
    private val reminderRunner: NotificationReminderRunner,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${notification.reminder.morning-cron}", zone = BUSINESS_TIME_ZONE)
    fun remindInactiveGroups() {
        val currentDate = clock.currentBusinessDate()
        reminderRunner.run(
            reminderType = NotificationType.GROUP_INACTIVE_60_DAYS,
            findTargetIds = { cursor, size -> groupReminderService.findInactiveGroupIds(currentDate, cursor, size) },
            remind = { targetId -> groupReminderService.remindInactiveGroup(targetId, currentDate) },
        )
    }
}
