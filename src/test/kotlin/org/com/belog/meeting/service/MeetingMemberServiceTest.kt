package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.service.GroupMemberFinder
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.MeetingMemberOptionResult
import org.com.belog.meeting.service.result.MeetingMemberResult
import org.com.belog.user.domain.User
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeetingMemberServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val meetingParticipantRepository = mock(MeetingParticipantRepository::class.java)
    private val userService = mock(UserService::class.java)
    private val meetingMemberService =
        MeetingMemberService(
            meetingRepository,
            GroupMemberFinder(groupMemberRepository),
            groupMemberRepository,
            meetingParticipantRepository,
            userService,
        )

    @Test
    fun `요청자를 먼저 반환하고 나머지는 그룹 멤버 ID 순으로 정렬한다`() {
        stubAccessibleMeeting()
        val first = stubParticipant(30L, 3L, "첫멤버", null)
        val requester = stubParticipant(50L, USER_ID, "요청자", "https://example.com/profile")
        val last = stubParticipant(70L, 7L, "끝멤버", null)
        `when`(meetingParticipantRepository.findAllActiveWithUserByMeetingId(MEETING_ID))
            .thenReturn(listOf(last, requester, first))

        val results = meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)

        assertEquals(
            listOf(
                MeetingMemberResult(50L, "요청자", "https://example.com/profile", true),
                MeetingMemberResult(30L, "첫멤버", null, false),
                MeetingMemberResult(70L, "끝멤버", null, false),
            ),
            results,
        )
    }

    @Test
    fun `만남에 참여하지 않은 그룹 멤버도 조회할 수 있지만 목록에는 추가하지 않는다`() {
        stubAccessibleMeeting()
        val first = stubParticipant(30L, 3L, "첫멤버", null)
        val last = stubParticipant(70L, 7L, "끝멤버", null)
        `when`(meetingParticipantRepository.findAllActiveWithUserByMeetingId(MEETING_ID))
            .thenReturn(listOf(last, first))

        val results = meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)

        assertEquals(
            listOf(
                MeetingMemberResult(30L, "첫멤버", null, false),
                MeetingMemberResult(70L, "끝멤버", null, false),
            ),
            results,
        )
    }

    @Test
    fun `조회 가능한 만남이 없으면 만남 조회 실패를 반환한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)
            }

        assertEquals(MeetingErrorCode.MEETING_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `활성 그룹 멤버가 아니면 만남 멤버를 조회할 수 없다`() {
        stubAccessibleMeeting()
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(GROUP_ID, USER_ID))
            .thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
    }

    @Test
    fun `참여자 선택 목록은 그룹 멤버별 참여 여부와 제외 가능 여부를 반환한다`() {
        val owner = stubParticipant(10L, USER_ID, "방장", null)
        val payer = stubParticipant(20L, 2L, "결제자", null)
        val participant = stubParticipant(30L, 3L, "참여자", null)
        val nonParticipant = stubParticipant(40L, 4L, "미참여자", null)
        stubOwnedMeeting(ownerMember = owner.groupMember)
        `when`(meetingParticipantRepository.findAllActiveWithUserByMeetingId(MEETING_ID))
            .thenReturn(listOf(owner, payer, participant))
        `when`(meetingParticipantRepository.findAllBillLogLinkedGroupMemberIds(MEETING_ID)).thenReturn(listOf(20L))
        val groupMembers = listOf(nonParticipant, participant, payer, owner).map { it.groupMember }
        `when`(groupMemberRepository.findAllWithUserByGroupId(GROUP_ID)).thenReturn(groupMembers)

        val results = meetingMemberService.getMeetingMemberOptions(MEETING_ID, USER_ID)

        assertEquals(
            listOf(
                MeetingMemberOptionResult(10L, "방장", null, isMe = true, isParticipant = true, isRemovable = false),
                MeetingMemberOptionResult(20L, "결제자", null, isMe = false, isParticipant = true, isRemovable = false),
                MeetingMemberOptionResult(30L, "참여자", null, isMe = false, isParticipant = true, isRemovable = true),
                MeetingMemberOptionResult(40L, "미참여자", null, isMe = false, isParticipant = false, isRemovable = false),
            ),
            results,
        )
    }

    @Test
    fun `방장이 아니면 참여자 선택 목록을 조회할 수 없다`() {
        val owner = stubParticipant(10L, 99L, "방장", null)
        stubOwnedMeeting(ownerMember = owner.groupMember)

        val exception =
            assertFailsWith<BusinessException> {
                meetingMemberService.getMeetingMemberOptions(MEETING_ID, USER_ID)
            }

        assertEquals(MeetingErrorCode.NOT_MEETING_OWNER, exception.errorCode)
    }

    private fun stubOwnedMeeting(ownerMember: GroupMember) {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        `when`(group.id).thenReturn(GROUP_ID)
        `when`(meeting.group).thenReturn(group)
        `when`(meeting.owner).thenReturn(ownerMember)
        `when`(meeting.createdBy).thenReturn(ownerMember)
        `when`(meetingRepository.findByIdWithGroupOwnerAndCreator(MEETING_ID)).thenReturn(meeting)
    }

    private fun stubAccessibleMeeting() {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        `when`(group.id).thenReturn(GROUP_ID)
        `when`(meeting.group).thenReturn(group)
        `when`(meetingRepository.findByIdWithGroup(MEETING_ID)).thenReturn(meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(GROUP_ID, USER_ID))
            .thenReturn(mock(GroupMember::class.java))
    }

    private fun stubParticipant(
        memberId: Long,
        userId: Long,
        nickname: String,
        profileImageUrl: String?,
    ): MeetingParticipant {
        val user = mock(User::class.java)
        val member = mock(GroupMember::class.java)
        val participant = mock(MeetingParticipant::class.java)
        `when`(user.id).thenReturn(userId)
        `when`(member.id).thenReturn(memberId)
        `when`(member.user).thenReturn(user)
        `when`(participant.groupMember).thenReturn(member)
        `when`(userService.resolveDisplayNickname(user)).thenReturn(nickname)
        `when`(userService.resolveProfileImageUrl(user)).thenReturn(profileImageUrl)
        return participant
    }

    companion object {
        private const val GROUP_ID = 1L
        private const val MEETING_ID = 2L
        private const val USER_ID = 15L
    }
}
