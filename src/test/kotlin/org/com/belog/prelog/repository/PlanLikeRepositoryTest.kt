package org.com.belog.prelog.repository

import jakarta.persistence.EntityManager
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanLike
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class PlanLikeRepositoryTest {
    @Autowired
    private lateinit var planLikeRepository: PlanLikeRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `요청한 계획들의 좋아요 수를 계획별로 집계하고 좋아요가 없는 계획은 제외한다`() {
        val context = saveContext()
        saveLikes(
            PlanLike.create(context.firstPlan, context.owner),
            PlanLike.create(context.firstPlan, context.member),
            PlanLike.create(context.secondPlan, context.member),
            PlanLike.create(context.outsidePagePlan, context.owner),
        )

        val counts = planLikeRepository.countByPlanIds(context.pagePlanIds)

        assertEquals(
            mapOf(
                requireNotNull(context.firstPlan.id) to 2L,
                requireNotNull(context.secondPlan.id) to 1L,
            ),
            counts.associate { it.planId to it.likeCount },
        )
    }

    @Test
    fun `요청한 계획 중 지정한 그룹 멤버가 좋아요한 계획 ID만 조회한다`() {
        val context = saveContext()
        saveLikes(
            PlanLike.create(context.firstPlan, context.owner),
            PlanLike.create(context.firstPlan, context.member),
            PlanLike.create(context.secondPlan, context.member),
            PlanLike.create(context.outsidePagePlan, context.owner),
        )

        val likedPlanIds =
            planLikeRepository.findLikedPlanIds(
                planIds = context.pagePlanIds,
                groupMemberId = requireNotNull(context.owner.id),
            )

        assertEquals(listOf(requireNotNull(context.firstPlan.id)), likedPlanIds)
    }

    private fun saveContext(): PlanContext {
        val group = persist(Group.create("여행 모임", null, InviteCode.create("AB12CD")))
        val owner = persist(GroupMember.createOwner(group, saveUser("owner")))
        val member = persist(GroupMember.createMember(group, saveUser("member")))
        val currentDate = LocalDate.of(2026, 9, 22)
        val meeting =
            persist(
                Meeting.createFixed(
                    group = group,
                    creator = owner,
                    name = "광주 여행",
                    location = "광주",
                    dateRange = MeetingDateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)),
                    confirmedAt = Instant.parse("2026-09-22T00:00:00Z"),
                    currentDate = currentDate,
                ),
            )

        fun savePlan(title: String): Plan =
            persist(
                Plan.createMemo(
                    meeting = meeting,
                    creator = owner,
                    category = PlanCategory.RESTAURANT,
                    title = title,
                    content = "계획 내용",
                    currentDate = currentDate,
                ),
            )

        return PlanContext(
            owner = owner,
            member = member,
            firstPlan = savePlan("첫 번째 계획"),
            secondPlan = savePlan("두 번째 계획"),
            unlikedPlan = savePlan("좋아요 없는 계획"),
            outsidePagePlan = savePlan("다른 페이지 계획"),
        )
    }

    private fun saveUser(subject: String): User {
        val user =
            persist(
                User.createSocialUser(
                    email = "$subject@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = subject,
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = subject,
            name = "홍길동",
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return user
    }

    private fun saveLikes(vararg likes: PlanLike) {
        planLikeRepository.saveAllAndFlush(likes.toList())
        entityManager.clear()
    }

    private fun <T : Any> persist(entity: T): T {
        entityManager.persist(entity)
        return entity
    }

    private data class PlanContext(
        val owner: GroupMember,
        val member: GroupMember,
        val firstPlan: Plan,
        val secondPlan: Plan,
        val unlikedPlan: Plan,
        val outsidePagePlan: Plan,
    ) {
        val pagePlanIds: List<Long>
            get() = listOf(firstPlan, secondPlan, unlikedPlan).map { requireNotNull(it.id) }
    }
}
