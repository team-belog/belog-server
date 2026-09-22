package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.prelog.service.result.PlanListItemResult
import org.com.belog.prelog.service.result.PlanListResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate

@Service
class PlanService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val planRepository: PlanRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getPlans(
        meetingId: Long,
        userId: Long,
        category: PlanCategory?,
        pinnedOnly: Boolean,
        cursor: Long?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): PlanListResult {
        require(size in 1..MAX_PAGE_SIZE) { "조회 개수는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다." }
        require(cursor == null || cursor > 0) { "커서는 양수여야 합니다." }

        val meeting = findMeeting(meetingId)
        val groupMember = findGroupMember(meeting, userId)

        if (pinnedOnly) {
            return PlanListResult(items = emptyList(), nextCursor = null, hasNext = false)
        }

        val plans =
            planRepository.findPageWithCreator(
                meetingId = meetingId,
                category = category,
                cursor = cursor,
                pageable = PageRequest.of(0, size + 1),
            )
        val hasNext = plans.size > size
        val pagePlans = if (hasNext) plans.take(size) else plans
        val loginGroupMemberId = checkNotNull(groupMember.id) { "로그인 사용자의 그룹 멤버 ID가 없습니다." }
        val isGroupCreator = groupMember.role == GroupRole.OWNER

        return PlanListResult(
            items =
                pagePlans.map { plan ->
                    plan.toListItemResult(
                        loginGroupMemberId = loginGroupMemberId,
                        isGroupCreator = isGroupCreator,
                    )
                },
            nextCursor = if (hasNext) pagePlans.lastOrNull()?.id else null,
            hasNext = hasNext,
        )
    }

    @Transactional
    fun createLinkPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
    ): Plan {
        val meeting = findMeeting(meetingId)
        val creator = findGroupMember(meeting, creatorUserId)
        val currentDate = clock.currentBusinessDate()
        validateMeetingNotEnded(meeting, currentDate)

        return savePlan {
            Plan.createLink(
                meeting = meeting,
                creator = creator,
                category = category,
                title = title,
                url = url,
                currentDate = currentDate,
            )
        }
    }

    @Transactional
    fun createMemoPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        content: String,
    ): Plan {
        val meeting = findMeeting(meetingId)
        val creator = findGroupMember(meeting, creatorUserId)
        val currentDate = clock.currentBusinessDate()
        validateMeetingNotEnded(meeting, currentDate)

        return savePlan {
            Plan.createMemo(
                meeting = meeting,
                creator = creator,
                category = category,
                title = title,
                content = content,
                currentDate = currentDate,
            )
        }
    }

    private fun findMeeting(meetingId: Long): Meeting =
        meetingRepository.findById(meetingId).orElseThrow {
            BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        }

    private fun findGroupMember(
        meeting: Meeting,
        userId: Long,
    ): GroupMember {
        val groupId = checkNotNull(meeting.group.id) { "계획 대상 만남의 그룹 ID가 없습니다." }
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun validateMeetingNotEnded(
        meeting: Meeting,
        currentDate: LocalDate,
    ) {
        if (meeting.isEnded(currentDate)) {
            throw BusinessException(PreLogErrorCode.MEETING_ALREADY_ENDED)
        }
    }

    private fun savePlan(createPlan: () -> Plan): Plan {
        val plan =
            try {
                createPlan()
            } catch (exception: IllegalArgumentException) {
                throw BusinessException(PreLogErrorCode.INVALID_PLAN, exception)
            }

        return planRepository.save(plan)
    }

    private fun Plan.toListItemResult(
        loginGroupMemberId: Long,
        isGroupCreator: Boolean,
    ): PlanListItemResult {
        val planId = checkNotNull(id) { "조회된 계획의 ID가 없습니다." }
        val creatorId = checkNotNull(createdBy.id) { "계획 작성자의 그룹 멤버 ID가 없습니다." }
        val isPlanCreator = creatorId == loginGroupMemberId

        return PlanListItemResult(
            planId = planId,
            type = type,
            category = category,
            title = title,
            url = url,
            address = null,
            thumbnailUrl = null,
            likeCount = 0,
            likedByMe = false,
            pinned = false,
            canDelete = isPlanCreator || isGroupCreator,
            createdAt = checkNotNull(createdAt) { "조회된 계획의 생성 시각이 없습니다." },
        )
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
    }
}
