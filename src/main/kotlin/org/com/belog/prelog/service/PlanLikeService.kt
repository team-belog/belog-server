package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.notification.service.NotificationService
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.prelog.service.result.PlanLikeResult
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlanLikeService(
    private val groupMemberRepository: GroupMemberRepository,
    private val planRepository: PlanRepository,
    private val planLikeRepository: PlanLikeRepository,
    private val notificationService: NotificationService,
    private val userService: UserService,
) {
    @Transactional
    fun likePlan(
        planId: Long,
        userId: Long,
    ): PlanLikeResult {
        val target = findLikeTarget(planId, userId)

        planLikeRepository.saveIfAbsent(
            planId = target.planId,
            groupMemberId = target.groupMemberId,
        )
        notifyPlanLiked(target)

        return createResult(
            planId = target.planId,
            likedByMe = true,
        )
    }

    @Transactional
    fun unlikePlan(
        planId: Long,
        userId: Long,
    ): PlanLikeResult {
        val target = findLikeTarget(planId, userId)

        planLikeRepository.deleteByPlanIdAndGroupMemberId(
            planId = target.planId,
            groupMemberId = target.groupMemberId,
        )

        return createResult(
            planId = target.planId,
            likedByMe = false,
        )
    }

    private fun findLikeTarget(
        planId: Long,
        userId: Long,
    ): PlanLikeTarget {
        val plan = findPlan(planId)
        val meeting = plan.meeting
        val groupMember = findGroupMember(meeting, userId)

        return PlanLikeTarget(
            plan = plan,
            groupMember = groupMember,
            planId = checkNotNull(plan.id) { "좋아요 대상 계획의 ID가 없습니다." },
            groupMemberId = checkNotNull(groupMember.id) { "로그인 사용자의 그룹 멤버 ID가 없습니다." },
        )
    }

    private fun notifyPlanLiked(target: PlanLikeTarget) {
        val planAuthor = target.plan.createdBy
        if (planAuthor.id == target.groupMemberId || !planAuthor.isActive) {
            return
        }

        val likerUser = target.groupMember.user
        notificationService.create(
            PreLogNotificationCommands.planLiked(
                meetingId = checkNotNull(target.plan.meeting.id) { "좋아요 대상 만남의 ID가 없습니다." },
                planId = target.planId,
                likerGroupMemberId = target.groupMemberId,
                recipientUserId = checkNotNull(planAuthor.user.id) { "계획 작성자의 사용자 ID가 없습니다." },
                likerUserId = checkNotNull(likerUser.id) { "좋아요한 사용자의 ID가 없습니다." },
                likerNickname = userService.resolveDisplayNickname(likerUser),
            ),
        )
    }

    private fun findGroupMember(
        meeting: Meeting,
        userId: Long,
    ): GroupMember {
        val groupId = checkNotNull(meeting.group.id) { "좋아요 대상 만남의 그룹 ID가 없습니다." }
        return groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun findPlan(planId: Long): Plan =
        planRepository.findByIdWithMeetingAndCreator(planId)
            ?: throw BusinessException(PreLogErrorCode.PLAN_NOT_FOUND)

    private fun createResult(
        planId: Long,
        likedByMe: Boolean,
    ): PlanLikeResult =
        PlanLikeResult(
            planId = planId,
            likedByMe = likedByMe,
            likeCount = planLikeRepository.findAllByPlanIdForShare(planId).size.toLong(),
        )

    private data class PlanLikeTarget(
        val plan: Plan,
        val groupMember: GroupMember,
        val planId: Long,
        val groupMemberId: Long,
    )
}
