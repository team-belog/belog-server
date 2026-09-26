package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.prelog.service.result.PlanLikeResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlanLikeService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val planRepository: PlanRepository,
    private val planLikeRepository: PlanLikeRepository,
) {
    @Transactional
    fun likePlan(
        meetingId: Long,
        planId: Long,
        userId: Long,
    ): PlanLikeResult {
        val target = findLikeTarget(meetingId, planId, userId)

        planLikeRepository.saveIfAbsent(
            planId = target.planId,
            groupMemberId = target.groupMemberId,
        )

        return createResult(
            planId = target.planId,
            likedByMe = true,
        )
    }

    @Transactional
    fun unlikePlan(
        meetingId: Long,
        planId: Long,
        userId: Long,
    ): PlanLikeResult {
        val target = findLikeTarget(meetingId, planId, userId)

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
        meetingId: Long,
        planId: Long,
        userId: Long,
    ): PlanLikeTarget {
        val meeting = findMeeting(meetingId)
        val groupMember = findGroupMember(meeting, userId)
        val plan = findPlan(meetingId, planId)

        return PlanLikeTarget(
            planId = checkNotNull(plan.id) { "좋아요 대상 계획의 ID가 없습니다." },
            groupMemberId = checkNotNull(groupMember.id) { "로그인 사용자의 그룹 멤버 ID가 없습니다." },
        )
    }

    private fun findMeeting(meetingId: Long): Meeting =
        meetingRepository.findById(meetingId).orElseThrow {
            BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        }

    private fun findGroupMember(
        meeting: Meeting,
        userId: Long,
    ): GroupMember {
        val groupId = checkNotNull(meeting.group.id) { "좋아요 대상 만남의 그룹 ID가 없습니다." }
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun findPlan(
        meetingId: Long,
        planId: Long,
    ): Plan =
        planRepository.findByIdAndMeetingId(planId, meetingId)
            ?: throw BusinessException(PreLogErrorCode.PLAN_NOT_FOUND)

    private fun createResult(
        planId: Long,
        likedByMe: Boolean,
    ): PlanLikeResult =
        PlanLikeResult(
            planId = planId,
            likedByMe = likedByMe,
            likeCount = planLikeRepository.countByPlanId(planId),
        )

    private data class PlanLikeTarget(
        val planId: Long,
        val groupMemberId: Long,
    )
}
