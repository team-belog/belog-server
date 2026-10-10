package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.NotificationService
import org.com.belog.notification.service.command.CreateNotificationCommand
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanLike
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.user.domain.User
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanLikeServiceTest {
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val planLikeRepository = mock(PlanLikeRepository::class.java)
    private val notificationService = mock(NotificationService::class.java)
    private val userService = mock(UserService::class.java)
    private val planLikeService =
        PlanLikeService(
            groupMemberRepository = groupMemberRepository,
            planRepository = planRepository,
            planLikeRepository = planLikeRepository,
            notificationService = notificationService,
            userService = userService,
        )

    @Test
    fun `다른 멤버가 계획에 좋아요하면 작성자에게 좋아요 알림을 저장한다`() {
        val planAuthor = groupMember(groupMemberId = AUTHOR_GROUP_MEMBER_ID, userId = AUTHOR_USER_ID)
        stubLikeTarget(planAuthor = planAuthor)

        planLikeService.likePlan(PLAN_ID, USER_ID)

        verify(notificationService).create(
            CreateNotificationCommand(
                recipientUserId = AUTHOR_USER_ID,
                actorUserId = USER_ID,
                type = NotificationType.PRE_LOG_PLAN_LIKED,
                message = "좋아요한 멤버 님이 좋아요를 눌렀어요",
                targetId = MEETING_ID,
                deduplicationKey = "PRE_LOG_PLAN_LIKED:$PLAN_ID:$GROUP_MEMBER_ID",
            ),
        )
    }

    @Test
    fun `작성자가 자신의 계획에 좋아요하면 알림을 저장하지 않는다`() {
        stubLikeTarget()

        planLikeService.likePlan(PLAN_ID, USER_ID)

        verifyNoInteractions(notificationService)
    }

    @Test
    fun `이미 좋아요한 계획에 다시 등록해도 현재 상태와 개수를 반환한다`() {
        stubLikeTarget()
        `when`(planLikeRepository.findAllByPlanIdForShare(PLAN_ID)).thenReturn(listOf(mock(PlanLike::class.java)))

        val firstResult = planLikeService.likePlan(PLAN_ID, USER_ID)
        val secondResult = planLikeService.likePlan(PLAN_ID, USER_ID)

        assertTrue(firstResult.likedByMe)
        assertEquals(1L, firstResult.likeCount)
        assertTrue(secondResult.likedByMe)
        assertEquals(1L, secondResult.likeCount)
        verify(planLikeRepository, times(2)).saveIfAbsent(PLAN_ID, GROUP_MEMBER_ID)
    }

    @Test
    fun `좋아요하지 않은 계획의 취소를 반복해도 성공한다`() {
        stubLikeTarget()
        `when`(planLikeRepository.findAllByPlanIdForShare(PLAN_ID)).thenReturn(emptyList())

        val firstResult = planLikeService.unlikePlan(PLAN_ID, USER_ID)
        val secondResult = planLikeService.unlikePlan(PLAN_ID, USER_ID)

        assertFalse(firstResult.likedByMe)
        assertEquals(0L, firstResult.likeCount)
        assertFalse(secondResult.likedByMe)
        assertEquals(0L, secondResult.likeCount)
        verify(planLikeRepository, times(2)).deleteByPlanIdAndGroupMemberId(PLAN_ID, GROUP_MEMBER_ID)
    }

    @Test
    fun `만남 참여 여부와 관계없이 그룹 멤버는 좋아요를 등록하고 본인 좋아요를 취소할 수 있다`() {
        stubLikeTarget()
        `when`(planLikeRepository.findAllByPlanIdForShare(PLAN_ID))
            .thenReturn(listOf(mock(PlanLike::class.java)), emptyList())

        val likeResult = planLikeService.likePlan(PLAN_ID, USER_ID)
        val unlikeResult = planLikeService.unlikePlan(PLAN_ID, USER_ID)

        assertTrue(likeResult.likedByMe)
        assertFalse(unlikeResult.likedByMe)
        verify(planLikeRepository).saveIfAbsent(PLAN_ID, GROUP_MEMBER_ID)
        verify(planLikeRepository).deleteByPlanIdAndGroupMemberId(PLAN_ID, GROUP_MEMBER_ID)
    }

    @Test
    fun `그룹 비멤버는 좋아요를 등록하거나 취소할 수 없다`() {
        val meeting = mock(Meeting::class.java)
        val group = mock(Group::class.java)
        `when`(meeting.group).thenReturn(group)
        `when`(group.id).thenReturn(GROUP_ID)
        val plan = mock(Plan::class.java)
        `when`(plan.meeting).thenReturn(meeting)
        `when`(planRepository.findByIdWithMeetingAndCreator(PLAN_ID)).thenReturn(plan)
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(GROUP_ID, USER_ID)).thenReturn(null)

        val likeException =
            assertFailsWith<BusinessException> {
                planLikeService.likePlan(PLAN_ID, USER_ID)
            }
        val unlikeException =
            assertFailsWith<BusinessException> {
                planLikeService.unlikePlan(PLAN_ID, USER_ID)
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, likeException.errorCode)
        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, unlikeException.errorCode)
        verifyNoInteractions(planLikeRepository)
    }

    private fun stubLikeTarget(planAuthor: GroupMember? = null) {
        val meeting = mock(Meeting::class.java)
        val group = mock(Group::class.java)
        val groupMember = groupMember(groupMemberId = GROUP_MEMBER_ID, userId = USER_ID)
        val plan = mock(Plan::class.java)
        `when`(meeting.id).thenReturn(MEETING_ID)
        `when`(meeting.group).thenReturn(group)
        `when`(group.id).thenReturn(GROUP_ID)
        `when`(userService.resolveDisplayNickname(groupMember.user)).thenReturn("좋아요한 멤버")
        `when`(plan.id).thenReturn(PLAN_ID)
        `when`(plan.meeting).thenReturn(meeting)
        `when`(plan.createdBy).thenReturn(planAuthor ?: groupMember)
        `when`(groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(GROUP_ID, USER_ID)).thenReturn(groupMember)
        `when`(planRepository.findByIdWithMeetingAndCreator(PLAN_ID)).thenReturn(plan)
    }

    private fun groupMember(
        groupMemberId: Long,
        userId: Long,
    ): GroupMember {
        val user = mock(User::class.java)
        val groupMember = mock(GroupMember::class.java)
        `when`(user.id).thenReturn(userId)
        `when`(groupMember.id).thenReturn(groupMemberId)
        `when`(groupMember.user).thenReturn(user)
        `when`(groupMember.isActive).thenReturn(true)
        return groupMember
    }

    companion object {
        private const val PLAN_ID = 12L
        private const val MEETING_ID = 1L
        private const val GROUP_ID = 3L
        private const val GROUP_MEMBER_ID = 10L
        private const val USER_ID = 15L
        private const val AUTHOR_GROUP_MEMBER_ID = 11L
        private const val AUTHOR_USER_ID = 16L
    }
}
