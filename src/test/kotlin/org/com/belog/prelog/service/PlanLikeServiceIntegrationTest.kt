package org.com.belog.prelog.service

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.repository.NotificationOutboxRepository
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanLike
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.prelog.service.result.PlanLikeResult
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlanLikeServiceIntegrationTest {
    @Autowired
    private lateinit var planService: PlanService

    @Autowired
    private lateinit var planLikeService: PlanLikeService

    @Autowired
    private lateinit var planLikeRepository: PlanLikeRepository

    @Autowired
    private lateinit var planRepository: PlanRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @AfterEach
    fun cleanUp() {
        notificationOutboxRepository.deleteAllInBatch()
        notificationRepository.deleteAllInBatch()
        planLikeRepository.deleteAll()
        planRepository.deleteAll()
        meetingRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `동일한 그룹 멤버와 계획의 좋아요는 고유 제약으로 중복 저장되지 않는다`() {
        val context = savePlanContext()
        planLikeRepository.saveAndFlush(PlanLike.create(context.plan, context.groupMember))

        assertFailsWith<DataIntegrityViolationException> {
            planLikeRepository.saveAndFlush(PlanLike.create(context.plan, context.groupMember))
        }

        assertEquals(1L, planLikeRepository.count())
    }

    @Test
    fun `동일한 좋아요 등록을 반복하면 멱등하게 성공하고 한 건만 유지한다`() {
        val context = savePlanContext()

        val firstResult = planLikeService.likePlan(context.planId, context.userId)
        val secondResult = planLikeService.likePlan(context.planId, context.userId)

        assertTrue(firstResult.likedByMe)
        assertEquals(1L, firstResult.likeCount)
        assertTrue(secondResult.likedByMe)
        assertEquals(1L, secondResult.likeCount)
        assertEquals(1L, planLikeRepository.count())
    }

    @Test
    fun `동일한 사용자가 동시에 좋아요를 등록해도 한 건만 생성된다`() {
        val context = savePlanContext()
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val requests =
                (1..CONCURRENT_REQUEST_COUNT).map {
                    executor.submit<PlanLikeResult> {
                        startSignal.await()
                        planLikeService.likePlan(context.planId, context.userId)
                    }
                }

            startSignal.countDown()
            val results = requests.map { request -> request.get(10, TimeUnit.SECONDS) }

            assertTrue(results.all { result -> result.likedByMe })
            assertTrue(results.all { result -> result.likeCount == 1L })
            assertEquals(1L, planLikeRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `좋아요를 취소했다가 다시 눌러도 작성자에게 좋아요 알림은 한 건만 저장된다`() {
        val context = savePlanContext()
        val liker = saveCompletedUser(providerUserId = "liker-subject", email = "liker@example.com", nickname = "좋아요멤버")
        val likerMember = groupMemberRepository.saveAndFlush(GroupMember.createMember(context.plan.meeting.group, liker))
        val likerUserId = requireNotNull(liker.id)

        planLikeService.likePlan(context.planId, likerUserId)
        planLikeService.unlikePlan(context.planId, likerUserId)
        planLikeService.likePlan(context.planId, likerUserId)

        val notification =
            notificationRepository
                .findAll()
                .single { notification -> notification.type == NotificationType.PRE_LOG_PLAN_LIKED }
        assertEquals("PRE_LOG_PLAN_LIKED:${context.planId}:${likerMember.id}", notification.deduplicationKey)
        assertEquals("좋아요멤버 님이 좋아요를 눌렀어요", notification.message)
        assertEquals(context.meetingId, notification.targetId)
        assertEquals(1L, notificationOutboxRepository.count())
    }

    @Test
    fun `좋아요가 등록된 계획을 삭제하면 좋아요와 계획이 모두 삭제된다`() {
        val context = savePlanContext()
        planLikeRepository.saveAndFlush(PlanLike.create(context.plan, context.groupMember))

        planService.deletePlan(context.planId, context.userId)

        assertEquals(0L, planLikeRepository.count())
        assertFalse(planRepository.existsById(context.planId))
    }

    private fun savePlanContext(): PlanContext {
        val group = groupRepository.save(createGroup())
        val user = saveCompletedUser()
        val groupMember = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = groupMember,
                    name = "광주 여행",
                    location = "광주",
                    dateRange = MeetingDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)),
                    confirmedAt = Instant.parse("2026-09-22T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 22),
                ),
            )
        val plan =
            planRepository.saveAndFlush(
                Plan.createLocation(
                    meeting = meeting,
                    creator = groupMember,
                    category = PlanCategory.RESTAURANT,
                    title = "광주 맛집",
                    url = "https://example.com/place",
                    currentDate = LocalDate.of(2026, 9, 22),
                ),
            )

        return PlanContext(
            meetingId = requireNotNull(meeting.id),
            planId = requireNotNull(plan.id),
            userId = requireNotNull(user.id),
            groupMember = groupMember,
            plan = plan,
        )
    }

    private fun createGroup(): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun saveCompletedUser(
        providerUserId: String = "member-subject",
        email: String = "member@example.com",
        nickname: String = "멤버",
    ): User {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = email,
                    provider = SocialProvider.GOOGLE,
                    providerUserId = providerUserId,
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = nickname,
            name = "홍길동",
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    private data class PlanContext(
        val meetingId: Long,
        val planId: Long,
        val userId: Long,
        val groupMember: GroupMember,
        val plan: Plan,
    )

    companion object {
        private const val CONCURRENT_REQUEST_COUNT = 4

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
