package org.com.belog.prelog.domain

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class PlanTest {
    @Test
    fun `LINK 계획을 생성하면 URL만 저장한다`() {
        val context = createMeetingContext()

        val plan =
            Plan.createLink(
                meeting = context.meeting,
                creator = context.participant,
                category = PlanCategory.RESTAURANT,
                title = "  광주 맛집  ",
                url = "  https://example.com/place  ",
                currentDate = CURRENT_DATE,
            )

        assertSame(context.meeting, plan.meeting)
        assertSame(context.participant, plan.createdBy)
        assertEquals(PlanType.LINK, plan.type)
        assertEquals(PlanCategory.RESTAURANT, plan.category)
        assertEquals("광주 맛집", plan.title)
        assertEquals("https://example.com/place", plan.url)
        assertNull(plan.content)
    }

    @Test
    fun `MEMO 계획을 생성하면 내용만 저장한다`() {
        val context = createMeetingContext()

        val plan =
            Plan.createMemo(
                meeting = context.meeting,
                creator = context.participant,
                category = PlanCategory.ACTIVITY,
                title = "  일정 메모  ",
                content = "  여유롭게 출발하기  ",
                currentDate = CURRENT_DATE,
            )

        assertEquals(PlanType.MEMO, plan.type)
        assertEquals(PlanCategory.ACTIVITY, plan.category)
        assertEquals("일정 메모", plan.title)
        assertNull(plan.url)
        assertEquals("여유롭게 출발하기", plan.content)
    }

    @Test
    fun `다른 만남의 참여자는 계획을 생성할 수 없다`() {
        val context = createMeetingContext()
        val otherMeeting = createFixedMeeting(context.group, context.groupMember)
        val otherParticipant = MeetingParticipant.create(otherMeeting, context.groupMember)

        assertFailsWith<IllegalArgumentException> {
            Plan.createLink(
                meeting = context.meeting,
                creator = otherParticipant,
                category = PlanCategory.CAFE,
                title = "카페",
                url = "https://example.com/cafe",
                currentDate = CURRENT_DATE,
            )
        }
    }

    @Test
    fun `종료된 만남에는 계획을 생성할 수 없다`() {
        val context =
            createMeetingContext(
                startDate = CURRENT_DATE.minusDays(2),
                endDate = CURRENT_DATE.minusDays(1),
                meetingCreationDate = CURRENT_DATE.minusDays(2),
            )

        assertFailsWith<IllegalArgumentException> {
            Plan.createMemo(
                meeting = context.meeting,
                creator = context.participant,
                category = PlanCategory.OTHER,
                title = "메모",
                content = "종료된 만남 메모",
                currentDate = CURRENT_DATE,
            )
        }
    }

    @Test
    fun `HTTP 또는 HTTPS 형식이 아니거나 최대 길이를 초과한 URL은 사용할 수 없다`() {
        val context = createMeetingContext()
        val invalidUrls =
            listOf(
                "example.com/place",
                "/relative/place",
                "ftp://example.com/place",
                "https:///place",
                "https://example.com/${"a".repeat(Plan.URL_MAX_LENGTH)}",
            )

        invalidUrls.forEach { url ->
            assertFailsWith<IllegalArgumentException> {
                Plan.createLink(
                    meeting = context.meeting,
                    creator = context.participant,
                    category = PlanCategory.RESTAURANT,
                    title = "맛집",
                    url = url,
                    currentDate = CURRENT_DATE,
                )
            }
        }
    }

    @Test
    fun `제목이 공백이거나 최대 길이를 초과하면 계획을 생성할 수 없다`() {
        val context = createMeetingContext()

        listOf("   ", "가".repeat(Plan.TITLE_MAX_LENGTH + 1)).forEach { title ->
            assertFailsWith<IllegalArgumentException> {
                Plan.createMemo(
                    meeting = context.meeting,
                    creator = context.participant,
                    category = PlanCategory.OTHER,
                    title = title,
                    content = "내용",
                    currentDate = CURRENT_DATE,
                )
            }
        }
    }

    @Test
    fun `메모 내용이 공백이거나 최대 길이를 초과하면 계획을 생성할 수 없다`() {
        val context = createMeetingContext()

        listOf("   ", "가".repeat(Plan.CONTENT_MAX_LENGTH + 1)).forEach { content ->
            assertFailsWith<IllegalArgumentException> {
                Plan.createMemo(
                    meeting = context.meeting,
                    creator = context.participant,
                    category = PlanCategory.OTHER,
                    title = "메모",
                    content = content,
                    currentDate = CURRENT_DATE,
                )
            }
        }
    }

    private fun createMeetingContext(
        startDate: LocalDate = CURRENT_DATE,
        endDate: LocalDate = CURRENT_DATE.plusDays(1),
        meetingCreationDate: LocalDate = CURRENT_DATE,
    ): MeetingContext {
        val group = createGroup()
        val groupMember = GroupMember.createOwner(group, completedUser())
        val meeting = createFixedMeeting(group, groupMember, startDate, endDate, meetingCreationDate)
        val participant = MeetingParticipant.create(meeting, groupMember)
        return MeetingContext(group, groupMember, meeting, participant)
    }

    private fun createFixedMeeting(
        group: Group,
        creator: GroupMember,
        startDate: LocalDate = CURRENT_DATE,
        endDate: LocalDate = CURRENT_DATE.plusDays(1),
        currentDate: LocalDate = CURRENT_DATE,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
            dateRange = MeetingDateRange(startDate, endDate),
            confirmedAt = Instant.parse("2026-09-22T00:00:00Z"),
            currentDate = currentDate,
        )

    private fun createGroup(): Group =
        Group.create(
            name = "여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun completedUser(): User =
        User
            .createSocialUser(
                email = "user@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = "user-subject",
            ).apply {
                completeOnboarding(
                    profileImageObjectKey = null,
                    nickname = "참여자",
                    name = "홍길동",
                    bankAccount =
                        BankAccount.create(
                            bank = Bank.KB_KOOKMIN,
                            accountNumber = "123456789012",
                            accountHolderName = "홍길동",
                        ),
                    completedAt = Instant.parse("2026-09-20T00:00:00Z"),
                )
            }

    private data class MeetingContext(
        val group: Group,
        val groupMember: GroupMember,
        val meeting: Meeting,
        val participant: MeetingParticipant,
    )

    companion object {
        private val CURRENT_DATE: LocalDate = LocalDate.of(2026, 9, 22)
    }
}
