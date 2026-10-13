package org.com.belog.group.service

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.group.service.result.GroupMeetingParticipantResult
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.repository.MeetingScheduleResponseRepository
import org.com.belog.user.domain.User
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupDetailServiceTest {
    private val groupRepository = dependency<GroupRepository>()
    private val groupMemberRepository = dependency<GroupMemberRepository>()
    private val meetingRepository = dependency<MeetingRepository>()
    private val participantRepository = dependency<MeetingParticipantRepository>()
    private val responseRepository = dependency<MeetingScheduleResponseRepository>()
    private val userService = dependency<UserService>()
    private val clock = Clock.fixed(Instant.parse("2026-10-13T00:00:00Z"), ZoneOffset.UTC)
    private val service =
        GroupService(
            groupCreationAttemptService = dependency(),
            inviteCodeGenerator = dependency(),
            inviteLinkGenerator = dependency(),
            groupCoverImageObjectVerifier = dependency(),
            groupRepository = groupRepository,
            groupMemberRepository = groupMemberRepository,
            meetingRepository = meetingRepository,
            meetingParticipantRepository = participantRepository,
            meetingScheduleResponseRepository = responseRepository,
            settlementRequestService = dependency(),
            s3ObjectReadUrlProvider = dependency(),
            userService = userService,
            notificationService = dependency(),
            clock = clock,
        )
    private val group = Group.create("여행 모임", null, InviteCode.create("AB12CD"))
    private val groupOwner = member(1L, "그룹 방장", owner = true)
    private val meetingOwner = member(2L, "만남장")

    init {
        ReflectionTestUtils.setField(group, "id", 1L)
        `when`(groupRepository.findActiveById(1L)).thenReturn(group)
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(1L, 1L)).thenReturn(groupOwner)
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(1L, 2L)).thenReturn(meetingOwner)
    }

    @Test
    fun `그룹 방장 여부와 관계없이 요청자의 만남장 여부를 반환한다`() {
        stubMeetings()

        val ownerResult = service.getGroup(1L, 2L)
        val groupOwnerResult = service.getGroup(1L, 1L)

        assertEquals(listOf(GroupMeetingParticipantResult("만남장", true)), ownerResult.schedulingMeetings.single().participants)
        assertEquals(ownerResult.schedulingMeetings.single().participants, groupOwnerResult.schedulingMeetings.single().participants)
        assertEquals(
            listOf(GroupMeetingParticipantResult("만남장", true), GroupMeetingParticipantResult("그룹 방장", false)),
            ownerResult.activeMeetings.single().participants,
        )
        assertEquals(1, ownerResult.schedulingMeetings.single().participantCount)
        assertEquals(2, ownerResult.activeMeetings.single().participantCount)
        assertTrue(ownerResult.schedulingMeetings.single().hasResponded)
        assertFalse(groupOwnerResult.schedulingMeetings.single().hasResponded)
        assertTrue(ownerResult.schedulingMeetings.single().isMeetingOwner)
        assertTrue(ownerResult.activeMeetings.single().isMeetingOwner)
        assertFalse(groupOwnerResult.schedulingMeetings.single().isMeetingOwner)
        assertFalse(groupOwnerResult.activeMeetings.single().isMeetingOwner)
    }

    @Test
    fun `현재 만남장을 참여자 목록 맨 앞에 두고 위임 후에도 나머지 순서를 유지한다`() {
        val meetings = stubMeetings()
        val lastMember = member(3L, "마지막 참여자")
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(1L, 3L)).thenReturn(lastMember)
        val participants =
            meetings.flatMap { meeting ->
                listOf(groupOwner, meetingOwner, lastMember).map { MeetingParticipant.create(meeting, it) }
            }
        `when`(participantRepository.findAllWithUserByMeetingIdIn(listOf(10L, 11L))).thenReturn(participants)

        val initialResult = service.getGroup(1L, 1L)
        val beforeDelegation = initialResult.schedulingMeetings.single()
        assertEquals(beforeDelegation.participants, initialResult.activeMeetings.single().participants)
        assertEquals(beforeDelegation.participantCount, initialResult.activeMeetings.single().participantCount)
        assertEquals(listOf("만남장", "그룹 방장", "마지막 참여자"), beforeDelegation.participants.map { it.nickname })
        assertEquals(listOf(true, false, false), beforeDelegation.participants.map { it.isMeetingOwner })
        assertEquals(3, beforeDelegation.participantCount)

        meetings.forEach { it.delegateOwnerTo(lastMember) }

        val successorResult = service.getGroup(1L, 3L)
        val previousOwnerResult = service.getGroup(1L, 2L)
        val afterDelegation = successorResult.schedulingMeetings.single()
        assertEquals(afterDelegation.participants, successorResult.activeMeetings.single().participants)
        assertEquals(afterDelegation.participantCount, successorResult.activeMeetings.single().participantCount)
        assertFalse(successorResult.schedulingMeetings.single().hasResponded)
        assertTrue(previousOwnerResult.schedulingMeetings.single().hasResponded)
        assertTrue(successorResult.schedulingMeetings.single().isMeetingOwner)
        assertTrue(successorResult.activeMeetings.single().isMeetingOwner)
        assertFalse(previousOwnerResult.schedulingMeetings.single().isMeetingOwner)
        assertFalse(previousOwnerResult.activeMeetings.single().isMeetingOwner)
        assertEquals(listOf("마지막 참여자", "그룹 방장", "만남장"), afterDelegation.participants.map { it.nickname })
        assertEquals(listOf(true, false, false), afterDelegation.participants.map { it.isMeetingOwner })
        assertEquals(3, afterDelegation.participantCount)
    }

    @Test
    fun `만남장 닉네임에 기존 탈퇴 사용자 표시 정책을 적용한다`() {
        stubMeetings()
        `when`(meetingOwner.user.isActive).thenReturn(false)

        val result = service.getGroup(1L, 1L)

        assertEquals(listOf(GroupMeetingParticipantResult("탈퇴한 사용자", true)), result.schedulingMeetings.single().participants)
        val activeParticipant =
            result.activeMeetings
                .single()
                .participants
                .first()
        assertEquals(GroupMeetingParticipantResult("탈퇴한 사용자", true), activeParticipant)
    }

    private fun stubMeetings(): List<Meeting> {
        val scheduling = Meeting.createPoll(group, meetingOwner, "일정 조율", null)
        val active =
            Meeting.createFixed(
                group = group,
                creator = meetingOwner,
                name = "확정 만남",
                location = null,
                dateRange = MeetingDateRange(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21)),
                confirmedAt = clock.instant(),
                currentDate = LocalDate.of(2026, 10, 13),
            )
        ReflectionTestUtils.setField(scheduling, "id", 10L)
        ReflectionTestUtils.setField(active, "id", 11L)
        `when`(participantRepository.findAllWithUserByMeetingIdIn(listOf(10L, 11L)))
            .thenReturn(
                listOf(
                    MeetingParticipant.create(scheduling, meetingOwner),
                    MeetingParticipant.create(active, meetingOwner),
                    MeetingParticipant.create(active, groupOwner),
                ),
            )
        `when`(meetingRepository.findSchedulingMeetings(1L, MeetingStatus.SCHEDULING)).thenReturn(listOf(scheduling))
        `when`(meetingRepository.findActiveMeetings(1L, LocalDate.of(2026, 10, 13), MeetingStatus.CONFIRMED))
            .thenReturn(listOf(active))
        return listOf(scheduling, active)
    }

    private fun member(
        id: Long,
        nickname: String,
        owner: Boolean = false,
    ): GroupMember {
        val user = dependency<User>()
        `when`(user.isOnboardingCompleted).thenReturn(true)
        val member = if (owner) GroupMember.createOwner(group, user) else GroupMember.createMember(group, user)
        ReflectionTestUtils.setField(member, "id", id)
        `when`(user.isActive).thenReturn(true)
        `when`(user.nickname).thenReturn(nickname)
        `when`(userService.resolveDisplayNickname(user)).thenCallRealMethod()
        return member
    }

    private inline fun <reified T : Any> dependency(): T = mock(T::class.java)
}
