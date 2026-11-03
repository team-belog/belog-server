package org.com.belog.notification.domain

import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationTest {
    private val recipient = createUser("recipient")
    private val actor = createUser("actor")

    @Test
    fun `알림 문구와 중복 방지 키를 정규화해 생성한다`() {
        val notification = createNotification(message = "  새 계획이 등록됐어요  ", deduplicationKey = "  plan:1:user:1  ")

        assertEquals("새 계획이 등록됐어요", notification.message)
        assertEquals("plan:1:user:1", notification.deduplicationKey)
        assertFalse(notification.isRead)
    }

    @Test
    fun `알림 문구가 비어 있거나 최대 길이를 초과하면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            createNotification(message = "   ")
        }
        assertFailsWith<IllegalArgumentException> {
            createNotification(message = "가".repeat(Notification.MESSAGE_MAX_LENGTH + 1))
        }
    }

    @Test
    fun `중복 방지 키가 비어 있거나 최대 길이를 초과하면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            createNotification(deduplicationKey = "   ")
        }
        assertFailsWith<IllegalArgumentException> {
            createNotification(deduplicationKey = "a".repeat(Notification.DEDUPLICATION_KEY_MAX_LENGTH + 1))
        }
    }

    @Test
    fun `대상 ID가 필요한 알림에는 양수인 대상 ID가 필요하다`() {
        assertFailsWith<IllegalArgumentException> {
            createNotification(targetId = null)
        }
        assertFailsWith<IllegalArgumentException> {
            createNotification(targetId = 0L)
        }
    }

    @Test
    fun `그룹 삭제 알림에는 대상 ID가 없어야 한다`() {
        val notification =
            createNotification(
                type = NotificationType.GROUP_DELETED,
                targetId = null,
            )

        assertEquals(null, notification.targetId)
        assertFailsWith<IllegalArgumentException> {
            createNotification(
                type = NotificationType.GROUP_DELETED,
                targetId = 1L,
            )
        }
    }

    @Test
    fun `읽음 처리는 최초 시각을 유지하며 멱등하게 동작한다`() {
        val notification = createNotification()
        val firstReadAt = Instant.parse("2026-10-02T00:00:00Z")
        val secondReadAt = Instant.parse("2026-10-02T01:00:00Z")

        assertTrue(notification.markAsRead(firstReadAt))
        assertFalse(notification.markAsRead(secondReadAt))
        assertTrue(notification.isRead)
        assertEquals(firstReadAt, notification.readAt)
    }

    private fun createNotification(
        type: NotificationType = NotificationType.PRE_LOG_PLAN_CREATED,
        message: String = "새 계획이 등록됐어요",
        targetId: Long? = 1L,
        deduplicationKey: String = "plan:1:user:1",
    ): Notification =
        Notification.create(
            recipient = recipient,
            actor = actor,
            type = type,
            message = message,
            targetId = targetId,
            deduplicationKey = deduplicationKey,
        )

    private fun createUser(providerUserId: String): User =
        User.createSocialUser(
            email = "$providerUserId@example.com",
            provider = SocialProvider.GOOGLE,
            providerUserId = providerUserId,
        )
}
