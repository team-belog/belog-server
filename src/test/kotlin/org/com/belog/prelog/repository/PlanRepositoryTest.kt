package org.com.belog.prelog.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.hibernate.Hibernate
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class PlanRepositoryTest {
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
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `카테고리 필터를 적용해 계획을 조회한다`() {
        val context = saveMeetingContext()
        planRepository.saveAllAndFlush(
            listOf(
                createLinkPlan(context.meeting, context.owner, PlanCategory.RESTAURANT, "맛집"),
                createLinkPlan(context.meeting, context.owner, PlanCategory.ACCOMMODATION, "숙소"),
            ),
        )
        entityManager.clear()

        val plans =
            planRepository.findPageWithCreator(
                meetingId = requireNotNull(context.meeting.id),
                category = PlanCategory.ACCOMMODATION,
                cursor = null,
                pageable = PageRequest.of(0, 20),
            )

        assertEquals(listOf("숙소"), plans.map(Plan::title))
        assertTrue(plans.all { plan -> plan.category == PlanCategory.ACCOMMODATION })
    }

    @Test
    fun `계획 작성자를 추가 쿼리 없이 함께 조회한다`() {
        val context = saveMeetingContext()
        val firstMember = saveGroupMember(context.group, "first-member", "첫멤버")
        val secondMember = saveGroupMember(context.group, "second-member", "둘째멤버")
        planRepository.saveAllAndFlush(
            listOf(
                createLinkPlan(context.meeting, context.owner, PlanCategory.RESTAURANT, "첫 계획"),
                createLinkPlan(context.meeting, firstMember, PlanCategory.CAFE, "둘째 계획"),
                createLinkPlan(context.meeting, secondMember, PlanCategory.OTHER, "셋째 계획"),
            ),
        )
        entityManager.clear()
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val plans =
            planRepository.findPageWithCreator(
                meetingId = requireNotNull(context.meeting.id),
                category = null,
                cursor = null,
                pageable = PageRequest.of(0, 20),
            )
        val queryCountAfterPlanLookup = statistics.prepareStatementCount
        plans.forEach { plan -> plan.createdBy.role }

        assertEquals(3, plans.size)
        assertTrue(plans.all { plan -> Hibernate.isInitialized(plan.createdBy) })
        assertEquals(1, queryCountAfterPlanLookup)
        assertEquals(queryCountAfterPlanLookup, statistics.prepareStatementCount)
    }

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(createGroup())
        val owner = saveGroupOwner(group, "owner", "방장")
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = owner,
                    name = "광주 여행",
                    location = "광주",
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 24)),
                    confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 20),
                ),
            )
        return MeetingContext(group, owner, meeting)
    }

    private fun createLinkPlan(
        meeting: Meeting,
        creator: GroupMember,
        category: PlanCategory,
        title: String,
    ): Plan =
        Plan.createLink(
            meeting = meeting,
            creator = creator,
            category = category,
            title = title,
            url = "https://example.com/place",
            currentDate = LocalDate.of(2026, 9, 22),
        )

    private fun createGroup(): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun saveGroupOwner(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember = groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, saveCompletedUser(providerUserId, nickname)))

    private fun saveGroupMember(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, saveCompletedUser(providerUserId, nickname)))

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
    ): User {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "$providerUserId@example.com",
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

    private data class MeetingContext(
        val group: Group,
        val owner: GroupMember,
        val meeting: Meeting,
    )
}
