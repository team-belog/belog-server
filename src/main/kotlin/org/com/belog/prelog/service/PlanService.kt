package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.LocationResolutionStatus
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.prelog.service.query.PlanListCursor
import org.com.belog.prelog.service.result.MapPlanListItemResult
import org.com.belog.prelog.service.result.MapPlanListResult
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
    private val planLikeRepository: PlanLikeRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getPlans(
        meetingId: Long,
        userId: Long,
        category: PlanCategory?,
        pinnedOnly: Boolean,
        cursor: PlanListCursor?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): PlanListResult {
        require(size in 1..MAX_PAGE_SIZE) { "조회 개수는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다." }

        val meeting = findMeeting(meetingId)
        val groupMember = findGroupMember(meeting, userId)

        val plans =
            planRepository.findPageWithCreator(
                meetingId = meetingId,
                category = category,
                pinnedOnly = pinnedOnly,
                cursorPinned = cursor?.pinned,
                cursorId = cursor?.planId,
                pageable = PageRequest.of(0, size + 1),
            )
        val hasNext = plans.size > size
        val pagePlans = if (hasNext) plans.take(size) else plans
        val loginGroupMemberId = checkNotNull(groupMember.id) { "로그인 사용자의 그룹 멤버 ID가 없습니다." }
        val isMeetingCreator = meeting.isCreatedBy(groupMember)

        return PlanListResult(
            items =
                pagePlans.map { plan ->
                    plan.toListItemResult(
                        loginGroupMemberId = loginGroupMemberId,
                        isMeetingCreator = isMeetingCreator,
                    )
                },
            nextCursor =
                pagePlans
                    .lastOrNull()
                    ?.takeIf { hasNext }
                    ?.let { plan ->
                        PlanListCursor(
                            pinned = plan.pinned,
                            planId = checkNotNull(plan.id) { "커서 대상 계획의 ID가 없습니다." },
                        )
                    },
            hasNext = hasNext,
        )
    }

    @Transactional(readOnly = true)
    fun getMapPlans(
        meetingId: Long,
        userId: Long,
        category: PlanCategory?,
        cursor: Long?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): MapPlanListResult {
        require(size in 1..MAX_PAGE_SIZE) { "조회 개수는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다." }
        require(cursor == null || cursor > 0) { "커서는 양수여야 합니다." }

        val meeting = findMeeting(meetingId)
        findGroupMember(meeting, userId)

        val plans =
            planRepository.findMapPage(
                meetingId = meetingId,
                category = category,
                locationStatus = LocationResolutionStatus.RESOLVED,
                cursor = cursor,
                pageable = PageRequest.of(0, size + 1),
            )
        val hasNext = plans.size > size
        val pagePlans = if (hasNext) plans.take(size) else plans

        return MapPlanListResult(
            items = pagePlans.map { plan -> plan.toMapListItemResult() },
            nextCursor = if (hasNext) pagePlans.lastOrNull()?.id else null,
            hasNext = hasNext,
        )
    }

    @Transactional(readOnly = true)
    fun validatePlanCreation(
        meetingId: Long,
        userId: Long,
    ) {
        val meeting = findMeeting(meetingId)
        findGroupMember(meeting, userId)
        validateMeetingNotEnded(meeting, clock.currentBusinessDate())
    }

    @Transactional
    fun createLinkPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
        locationResolution: PlanLocationResolution = PlanLocationResolution.NotApplicable,
    ): Plan {
        val meeting = findMeeting(meetingId)
        val creator = findGroupMember(meeting, creatorUserId)
        val currentDate = clock.currentBusinessDate()
        validateMeetingNotEnded(meeting, currentDate)

        return savePlan {
            Plan
                .createLink(
                    meeting = meeting,
                    creator = creator,
                    category = category,
                    title = title,
                    url = url,
                    currentDate = currentDate,
                ).apply { applyLocationResolution(locationResolution) }
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

    @Transactional
    fun updateLinkPlan(
        planId: Long,
        userId: Long,
        category: PlanCategory,
        title: String,
        url: String,
        locationResolution: PlanLocationResolution? = null,
        expectedUrlWhenResolutionSkipped: String? = null,
    ): Plan =
        updatePlan(planId, userId) { plan ->
            if (expectedUrlWhenResolutionSkipped != null && plan.url != expectedUrlWhenResolutionSkipped) {
                throw BusinessException(PreLogErrorCode.PLAN_UPDATE_CONFLICT)
            }
            plan.updateLink(category = category, title = title, url = url)
            locationResolution?.let { resolution -> plan.applyLocationResolution(resolution) }
        }

    @Transactional(readOnly = true)
    fun getLinkUrlForUpdate(
        planId: Long,
        userId: Long,
    ): String? {
        val plan = findPlan(planId)
        val groupMember = findGroupMember(plan.meeting, userId)
        if (!plan.isCreatedBy(groupMember)) {
            throw BusinessException(PreLogErrorCode.PLAN_UPDATE_FORBIDDEN)
        }
        return plan.url
    }

    @Transactional
    fun updateMemoPlan(
        planId: Long,
        userId: Long,
        category: PlanCategory,
        title: String,
        content: String,
    ): Plan =
        updatePlan(planId, userId) { plan ->
            plan.updateMemo(category = category, title = title, content = content)
        }

    @Transactional
    fun deletePlan(
        planId: Long,
        userId: Long,
    ) {
        val plan = findPlan(planId)
        val meeting = plan.meeting
        val groupMember = findGroupMember(meeting, userId)

        if (!plan.isCreatedBy(groupMember) && !meeting.isCreatedBy(groupMember)) {
            throw BusinessException(PreLogErrorCode.PLAN_DELETE_FORBIDDEN)
        }

        planLikeRepository.deleteAllByPlanId(planId)
        planRepository.delete(plan)
    }

    @Transactional
    fun pinPlan(
        planId: Long,
        userId: Long,
    ) {
        findPinTarget(planId, userId).pin()
    }

    @Transactional
    fun unpinPlan(
        planId: Long,
        userId: Long,
    ) {
        findPinTarget(planId, userId).unpin()
    }

    private fun findMeeting(meetingId: Long): Meeting =
        meetingRepository.findActiveById(meetingId)
            ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)

    private fun findGroupMember(
        meeting: Meeting,
        userId: Long,
    ): GroupMember {
        val groupId = checkNotNull(meeting.group.id) { "계획 대상 만남의 그룹 ID가 없습니다." }
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun findPinTarget(
        planId: Long,
        userId: Long,
    ): Plan {
        val plan = findPlan(planId)
        val meeting = plan.meeting
        findGroupMember(meeting, userId)

        return plan
    }

    private fun updatePlan(
        planId: Long,
        userId: Long,
        update: (Plan) -> Unit,
    ): Plan {
        val plan = findPlan(planId)
        val meeting = plan.meeting
        val groupMember = findGroupMember(meeting, userId)

        if (!plan.isCreatedBy(groupMember)) {
            throw BusinessException(PreLogErrorCode.PLAN_UPDATE_FORBIDDEN)
        }

        try {
            update(plan)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(PreLogErrorCode.INVALID_PLAN, exception)
        }

        return plan
    }

    private fun findPlan(planId: Long): Plan =
        planRepository.findByIdWithMeetingAndCreator(planId)
            ?: throw BusinessException(PreLogErrorCode.PLAN_NOT_FOUND)

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
        isMeetingCreator: Boolean,
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
            address = location?.address,
            thumbnailUrl = null,
            likeCount = 0,
            likedByMe = false,
            pinned = pinned,
            canDelete = isPlanCreator || isMeetingCreator,
            createdAt = checkNotNull(createdAt) { "조회된 계획의 생성 시각이 없습니다." },
        )
    }

    private fun Plan.toMapListItemResult(): MapPlanListItemResult {
        val planLocation = checkNotNull(location) { "지도 계획의 위치 정보가 없습니다." }
        return MapPlanListItemResult(
            planId = checkNotNull(id) { "조회된 계획의 ID가 없습니다." },
            category = category,
            title = title,
            url = checkNotNull(url) { "지도 계획의 URL이 없습니다." },
            address = planLocation.address,
            latitude = checkNotNull(planLocation.latitude) { "지도 계획의 위도가 없습니다." },
            longitude = checkNotNull(planLocation.longitude) { "지도 계획의 경도가 없습니다." },
            pinned = pinned,
            createdAt = checkNotNull(createdAt) { "조회된 계획의 생성 시각이 없습니다." },
        )
    }

    private fun Plan.applyLocationResolution(resolution: PlanLocationResolution) {
        when (resolution) {
            PlanLocationResolution.NotApplicable -> Unit
            is PlanLocationResolution.Resolved -> resolveLocation(resolution.location)
            PlanLocationResolution.Failed -> failLocationResolution()
            is PlanLocationResolution.ProviderFailed ->
                failLocationResolution(
                    provider = resolution.provider,
                    externalPlaceId = resolution.externalPlaceId,
                    placeName = resolution.placeName,
                    address = resolution.address,
                )
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MAX_PAGE_SIZE = 50
    }
}
