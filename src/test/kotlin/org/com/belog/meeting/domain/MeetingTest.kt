package org.com.belog.meeting.domain

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MeetingTest {
    @Test
    fun `확정 날짜 만남을 생성하면 생성자와 날짜 정보를 저장하고 즉시 확정 상태가 된다`() {
        val group = createGroup("AB12CD")
        val creator = GroupMember.createMember(group, completedUser("creator-subject", "생성자"))
        val confirmedAt = Instant.parse("2026-09-20T00:00:00Z")

        val meeting =
            Meeting.createFixed(
                group = group,
                creator = creator,
                name = "  광주 1박 2일  ",
                location = "  서울고속버스터미널  ",
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22)),
                confirmedAt = confirmedAt,
                currentDate = LocalDate.of(2026, 9, 20),
            )

        assertSame(group, meeting.group)
        assertSame(creator, meeting.createdBy)
        assertEquals("광주 1박 2일", meeting.name)
        assertEquals("서울고속버스터미널", meeting.location)
        assertEquals(MeetingScheduleType.FIXED, meeting.scheduleType)
        assertEquals(MeetingStatus.CONFIRMED, meeting.status)
        assertEquals(LocalDate.of(2026, 9, 21), meeting.startDate)
        assertEquals(LocalDate.of(2026, 9, 22), meeting.endDate)
        assertEquals(confirmedAt, meeting.confirmedAt)
    }

    @Test
    fun `일정 조율 만남을 생성하면 날짜가 정해지지 않은 조율 중 상태가 된다`() {
        val group = createGroup("AB12CD")
        val creator = GroupMember.createMember(group, completedUser("creator-subject", "생성자"))

        val meeting =
            Meeting.createPoll(
                group = group,
                creator = creator,
                name = "  광주 여행  ",
                location = "  서울고속버스터미널  ",
            )

        assertSame(group, meeting.group)
        assertSame(creator, meeting.createdBy)
        assertEquals("광주 여행", meeting.name)
        assertEquals("서울고속버스터미널", meeting.location)
        assertEquals(MeetingScheduleType.POLL, meeting.scheduleType)
        assertEquals(MeetingStatus.SCHEDULING, meeting.status)
        assertNull(meeting.startDate)
        assertNull(meeting.endDate)
        assertNull(meeting.confirmedAt)
    }

    @Test
    fun `일정 조율 만남에 후보 일정 범위를 생성한다`() {
        val meeting = createPollMeeting()

        val candidateDateRange =
            MeetingCandidateDateRange.create(
                meeting = meeting,
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22)),
                currentDate = LocalDate.of(2026, 9, 20),
            )

        assertSame(meeting, candidateDateRange.meeting)
        assertEquals(LocalDate.of(2026, 9, 21), candidateDateRange.startDate)
        assertEquals(LocalDate.of(2026, 9, 22), candidateDateRange.endDate)
    }

    @Test
    fun `확정 날짜 만남에는 후보 일정 범위를 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            MeetingCandidateDateRange.create(
                meeting = createFixedMeeting(),
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22)),
                currentDate = LocalDate.of(2026, 9, 20),
            )
        }
    }

    @Test
    fun `과거 날짜로 후보 일정 범위를 생성할 수 없다`() {
        val meeting = createPollMeeting()

        assertFailsWith<IllegalArgumentException> {
            MeetingCandidateDateRange.create(
                meeting = meeting,
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 20)),
                currentDate = LocalDate.of(2026, 9, 20),
            )
        }
    }

    @Test
    fun `빈 장소는 장소가 없는 만남으로 정규화한다`() {
        val meeting = createFixedMeeting(location = "   ")

        assertNull(meeting.location)
    }

    @Test
    fun `만남명이 비어 있거나 15자를 초과하면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> { createFixedMeeting(name = "   ") }
        assertFailsWith<IllegalArgumentException> { createFixedMeeting(name = "가".repeat(16)) }
    }

    @Test
    fun `만남 장소가 20자를 초과하면 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            createFixedMeeting(location = "가".repeat(21))
        }
    }

    @Test
    fun `과거 날짜로 확정 날짜 만남을 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            createFixedMeeting(
                startDate = LocalDate.of(2026, 9, 19),
                currentDate = LocalDate.of(2026, 9, 20),
            )
        }
    }

    @Test
    fun `일반 그룹 멤버도 만남을 생성할 수 있다`() {
        val group = createGroup("AB12CD")
        val member = GroupMember.createMember(group, completedUser("member-subject", "멤버"))

        val meeting = createFixedMeeting(group = group, creator = member)

        assertSame(member, meeting.createdBy)
    }

    @Test
    fun `다른 그룹의 방장은 만남을 생성할 수 없다`() {
        val group = createGroup("AB12CD")
        val otherGroup = createGroup("EF34GH")
        val otherGroupOwner = GroupMember.createOwner(otherGroup, completedUser("owner-subject", "다른방장"))

        assertFailsWith<IllegalArgumentException> {
            createFixedMeeting(group = group, creator = otherGroupOwner)
        }
    }

    @Test
    fun `일정 조율 만남의 후보 일정을 최종 일정으로 확정한다`() {
        val meeting = createPollMeeting()
        val candidateDateRange =
            createCandidateDateRange(
                meeting = meeting,
                startDate = LocalDate.of(2026, 9, 22),
                endDate = LocalDate.of(2026, 9, 23),
            )
        val confirmedAt = Instant.parse("2026-09-20T01:00:00Z")

        val changed =
            meeting.confirmDate(
                candidateDateRange = candidateDateRange,
                confirmedAt = confirmedAt,
                currentDate = LocalDate.of(2026, 9, 20),
            )

        assertTrue(changed)
        assertEquals(MeetingStatus.CONFIRMED, meeting.status)
        assertEquals(candidateDateRange.startDate, meeting.startDate)
        assertEquals(candidateDateRange.endDate, meeting.endDate)
        assertEquals(confirmedAt, meeting.confirmedAt)
    }

    @Test
    fun `이미 확정한 후보 일정으로 재요청하면 변경하지 않는다`() {
        val meeting = createPollMeeting()
        val candidateDateRange = createCandidateDateRange(meeting)
        val firstConfirmedAt = Instant.parse("2026-09-20T01:00:00Z")
        meeting.confirmDate(candidateDateRange, firstConfirmedAt, LocalDate.of(2026, 9, 20))

        val changed =
            meeting.confirmDate(
                candidateDateRange = candidateDateRange,
                confirmedAt = Instant.parse("2026-09-21T01:00:00Z"),
                currentDate = LocalDate.of(2026, 9, 21),
            )

        assertFalse(changed)
        assertEquals(firstConfirmedAt, meeting.confirmedAt)
    }

    @Test
    fun `확정된 만남의 일정을 변경하고 최초 확정 시각은 유지한다`() {
        val meeting =
            createFixedMeeting(
                startDate = LocalDate.of(2026, 9, 22),
                endDate = LocalDate.of(2026, 9, 23),
            )
        val confirmedAt = meeting.confirmedAt

        val changed =
            meeting.changeConfirmedDate(
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25)),
                currentDate = LocalDate.of(2026, 9, 21),
            )

        assertTrue(changed)
        assertEquals(LocalDate.of(2026, 9, 24), meeting.startDate)
        assertEquals(LocalDate.of(2026, 9, 25), meeting.endDate)
        assertEquals(confirmedAt, meeting.confirmedAt)
    }

    @Test
    fun `확정된 일정과 동일한 일정으로 재요청하면 변경하지 않는다`() {
        val meeting =
            createFixedMeeting(
                startDate = LocalDate.of(2026, 9, 20),
                endDate = LocalDate.of(2026, 9, 21),
            )

        val changed =
            meeting.changeConfirmedDate(
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21)),
                currentDate = LocalDate.of(2026, 9, 22),
            )

        assertFalse(changed)
    }

    @Test
    fun `종료된 만남의 일정은 변경할 수 없다`() {
        val meeting =
            createFixedMeeting(
                startDate = LocalDate.of(2026, 9, 20),
                endDate = LocalDate.of(2026, 9, 21),
            )

        assertFailsWith<IllegalArgumentException> {
            meeting.changeConfirmedDate(
                dateRange = MeetingDateRange(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 25)),
                currentDate = LocalDate.of(2026, 9, 22),
            )
        }
    }

    @Test
    fun `같은 그룹의 멤버를 만남 참여자로 생성한다`() {
        val group = createGroup("AB12CD")
        val owner = GroupMember.createOwner(group, completedUser("owner-subject", "방장"))
        val member = GroupMember.createMember(group, completedUser("member-subject", "멤버"))
        val meeting = createFixedMeeting(group = group, creator = owner)

        val participant = MeetingParticipant.create(meeting, member)

        assertSame(meeting, participant.meeting)
        assertSame(member, participant.groupMember)
    }

    @Test
    fun `다른 그룹의 멤버는 만남 참여자가 될 수 없다`() {
        val group = createGroup("AB12CD")
        val owner = GroupMember.createOwner(group, completedUser("owner-subject", "방장"))
        val meeting = createFixedMeeting(group = group, creator = owner)
        val otherGroup = createGroup("EF34GH")
        val otherMember = GroupMember.createMember(otherGroup, completedUser("member-subject", "다른멤버"))

        assertFailsWith<IllegalArgumentException> {
            MeetingParticipant.create(meeting, otherMember)
        }
    }

    private fun createFixedMeeting(
        group: Group = createGroup("AB12CD"),
        creator: GroupMember = GroupMember.createOwner(group, completedUser("owner-subject", "방장")),
        name: String = "광주 여행",
        location: String? = "서울고속버스터미널",
        startDate: LocalDate = LocalDate.of(2026, 9, 20),
        endDate: LocalDate = startDate,
        currentDate: LocalDate = LocalDate.of(2026, 9, 20),
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = name,
            location = location,
            dateRange = MeetingDateRange(startDate, endDate),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = currentDate,
        )

    private fun createPollMeeting(
        group: Group = createGroup("AB12CD"),
        creator: GroupMember = GroupMember.createOwner(group, completedUser("owner-subject", "방장")),
    ): Meeting =
        Meeting.createPoll(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = "서울고속버스터미널",
        )

    private fun createCandidateDateRange(
        meeting: Meeting,
        startDate: LocalDate = LocalDate.of(2026, 9, 22),
        endDate: LocalDate = LocalDate.of(2026, 9, 23),
    ): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            dateRange = MeetingDateRange(startDate, endDate),
            currentDate = LocalDate.of(2026, 9, 20),
        )

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun completedUser(
        providerUserId: String,
        nickname: String,
    ): User =
        User
            .createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ).apply {
                completeOnboarding(
                    profileImageObjectKey = null,
                    nickname = nickname,
                    bankAccount =
                        BankAccount.create(
                            bank = Bank.KB_KOOKMIN,
                            accountNumber = "123456789012",
                            accountHolderName = "홍길동",
                        ),
                    completedAt = Instant.parse("2026-09-15T00:00:00Z"),
                )
            }
}
