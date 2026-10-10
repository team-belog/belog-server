package org.com.belog.notification.worker

import org.com.belog.notification.config.NotificationReminderProperties
import org.com.belog.notification.domain.NotificationType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class NotificationReminderRunnerTest {
    private val runner = NotificationReminderRunner(NotificationReminderProperties(batchSize = 2))

    @Test
    fun `커서로 대상 ID를 끝까지 조회해 모든 대상에 리마인드한다`() {
        val targetIds = listOf(1L, 2L, 3L, 4L, 5L)
        val remindedIds = mutableListOf<Long>()

        runner.run(
            reminderType = NotificationType.MEETING_D7_REMINDER,
            findTargetIds = { cursor, size -> targetIds.filter { id -> cursor == null || id > cursor }.take(size) },
            remind = { targetId -> remindedIds.add(targetId) },
        )

        assertEquals(targetIds, remindedIds)
    }

    @Test
    fun `한 대상의 리마인드가 실패해도 나머지 대상은 계속 처리한다`() {
        val targetIds = listOf(1L, 2L, 3L)
        val remindedIds = mutableListOf<Long>()

        runner.run(
            reminderType = NotificationType.MEETING_D7_REMINDER,
            findTargetIds = { cursor, size -> targetIds.filter { id -> cursor == null || id > cursor }.take(size) },
            remind = { targetId ->
                check(targetId != 2L) { "리마인드 실패" }
                remindedIds.add(targetId)
            },
        )

        assertEquals(listOf(1L, 3L), remindedIds)
    }
}
