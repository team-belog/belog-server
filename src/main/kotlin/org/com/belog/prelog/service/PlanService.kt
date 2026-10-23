package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.notification.service.NotificationService
import org.com.belog.prelog.code.PreLogErrorCode
import org.com.belog.prelog.domain.LocationResolutionStatus
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.repository.PlanLikeRepository
import org.com.belog.prelog.repository.PlanRepository
import org.com.belog.prelog.service.command.PreLogNotificationCommands
import org.com.belog.prelog.service.query.PlanListCursor
import org.com.belog.prelog.service.result.MapPlanListItemResult
import org.com.belog.prelog.service.result.MapPlanListResult
import org.com.belog.prelog.service.result.PlanDetailResult
import org.com.belog.prelog.service.result.PlanListItemResult
import org.com.belog.prelog.service.result.PlanListResult
import org.com.belog.prelog.service.result.PlanLocationResolution
import org.com.belog.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlanService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val planRepository: PlanRepository,
    private val planLikeRepository: PlanLikeRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val notificationService: NotificationService,
    private val userService: UserService,
) {
    @Transactional(readOnly = true)
    fun getPlanDetail(
        planId: Long,
        userId: Long,
    ): PlanDetailResult {
        val plan = findPlan(planId)
        val groupMember = findGroupMember(plan.meeting, userId)
        if (plan.type != PlanType.MEMO) {
            throw BusinessException(PreLogErrorCode.PLAN_DETAIL_NOT_SUPPORTED)
        }

        return PlanDetailResult(
            planId = checkNotNull(plan.id) { "조회된 계획의 ID가 없습니다." },
            category = plan.category,
            title = plan.title,
            content = checkNotNull(plan.content) { "메모 계획의 본문이 없습니다." },
            canEdit = plan.isCreatedBy(groupMember),
        )
    }

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
        val planIds = pagePlans.map { plan -> checkNotNull(plan.id) { "조회된 계획의 ID가 없습니다." } }
        val likeCounts =
            if (planIds.isEmpty()) {
                emptyMap()
            } else {
                planLikeRepository.countByPlanIds(planIds).associate { count -> count.planId to count.likeCount }
            }
        val likedPlanIds =
            if (planIds.isEmpty()) {
                emptySet()
            } else {
                planLikeRepository.findLikedPlanIds(planIds, loginGroupMemberId).toSet()
            }

        return PlanListResult(
            items =
                pagePlans.mapIndexed { index, plan ->
                    val planId = planIds[index]
                    plan.toListItemResult(
                        loginGroupMemberId = loginGroupMemberId,
                        isMeetingCreator = isMeetingCreator,
                        likeCount = likeCounts[planId] ?: 0L,
                        likedByMe = planId in likedPlanIds,
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
        cursor: PlanListCursor?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): MapPlanListResult {
        require(size in 1..MAX_PAGE_SIZE) { "조회 개수는 1개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다." }

        val meeting = findMeeting(meetingId)
        findGroupMember(meeting, userId)

        val plans =
            planRepository.findMapPage(
                meetingId = meetingId,
                category = category,
                locationStatus = LocationResolutionStatus.RESOLVED,
                cursorPinned = cursor?.pinned,
                cursorId = cursor?.planId,
                pageable = PageRequest.of(0, size + 1),
            )
        val hasNext = plans.size > size
        val pagePlans = if (hasNext) plans.take(size) else plans

        return MapPlanListResult(
            items = pagePlans.map { plan -> plan.toMapListItemResult() },
            nextCursor =
                pagePlans
                    .lastOrNull()
                    ?.takeIf { hasNext }
                    ?.let { plan ->
                        PlanListCursor(
                            pinned = plan.pinned,
                            planId = checkNotNull(plan.id) { "커서 대상 지도 계획의 ID가 없습니다." },
                        )
                    },
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
        validateMeetingNotEnded(meeting)
    }

    @Transactional
    fun createLocationPlan(
        meetingId: Long,
        creatorUserId: Long,
        category: PlanCategory,
        title: String,
        url: String,
        locationResolution: PlanLocationResolution = PlanLocationResolution.NotApplicable,
    ): Plan {
        val meeting = findMeeting(meetingId)
        val creator = findGroupMember(meeting, creatorUserId)
        validateMeetingNotEnded(meeting)

        return savePlan {
            Plan
                .createLocation(
                    meeting = meeting,
                    creator = creator,
                    category = category,
                    title = title,
                    url = url,
                ).apply { applyLocationResolution(locationResolution) }
        }.also { plan -> notifyPlanCreated(meetingId, plan, creator) }
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
        validateMeetingNotEnded(meeting)

        return savePlan {
            Plan.createMemo(
                meeting = meeting,
                creator = creator,
                category = category,
                title = title,
                content = content,
            )
        }.also { plan -> notifyPlanCreated(meetingId, plan, creator) }
    }

    @Transactional
    fun updateMemoPlan(
        planId: Long,
        userId: Long,
        category: PlanCategory,
        title: String,
        content: String,
    ): Plan {
        val plan = findPlan(planId)
        val groupMember = findGroupMember(plan.meeting, userId)
        if (!plan.isCreatedBy(groupMember)) {
            throw BusinessException(PreLogErrorCode.PLAN_UPDATE_FORBIDDEN)
        }
        if (plan.type != PlanType.MEMO) {
            throw BusinessException(PreLogErrorCode.PLAN_UPDATE_NOT_SUPPORTED)
        }

        try {
            plan.updateMemo(category = category, title = title, content = content)
        } catch (exception: IllegalArgumentException) {
            throw BusinessException(PreLogErrorCode.INVALID_PLAN, exception)
        }

        return plan
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
        return groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
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

    private fun findPlan(planId: Long): Plan =
        planRepository.findByIdWithMeetingAndCreator(planId)
            ?: throw BusinessException(PreLogErrorCode.PLAN_NOT_FOUND)

    private fun validateMeetingNotEnded(meeting: Meeting) {
        if (meeting.isEnded()) {
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

    private fun notifyPlanCreated(
        meetingId: Long,
        plan: Plan,
        creator: GroupMember,
    ) {
        val planId = checkNotNull(plan.id) { "저장된 계획의 ID가 없습니다." }
        val creatorUser = creator.user
        val creatorUserId = checkNotNull(creatorUser.id) { "계획 작성자의 사용자 ID가 없습니다." }
        val creatorNickname = userService.resolveDisplayNickname(creatorUser)
        val recipientUserIds =
            meetingParticipantRepository
                .findAllActiveWithUserByMeetingId(meetingId)
                .map { participant -> checkNotNull(participant.groupMember.user.id) { "만남 참여자의 사용자 ID가 없습니다." } }
                .filter { recipientUserId -> recipientUserId != creatorUserId }

        notificationService.createAll(
            recipientUserIds.map { recipientUserId ->
                PreLogNotificationCommands.planCreated(
                    meetingId = meetingId,
                    planId = planId,
                    recipientUserId = recipientUserId,
                    creatorUserId = creatorUserId,
                    creatorNickname = creatorNickname,
                )
            },
        )
    }

    private fun Plan.toListItemResult(
        loginGroupMemberId: Long,
        isMeetingCreator: Boolean,
        likeCount: Long,
        likedByMe: Boolean,
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
            content =
                when (type) {
                    PlanType.LOCATION -> location?.address
                    PlanType.MEMO -> content
                },
            thumbnailUrl = null,
            likeCount = likeCount,
            likedByMe = likedByMe,
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
