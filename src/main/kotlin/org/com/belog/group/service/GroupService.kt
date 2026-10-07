package org.com.belog.group.service

import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GROUP_INVITE_CODE_UNIQUE_CONSTRAINT_NAME
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.infrastructure.GroupInviteLinkGenerator
import org.com.belog.group.infrastructure.RandomInviteCodeGenerator
import org.com.belog.group.infrastructure.S3GroupCoverImageObjectVerifier
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.group.service.query.GroupListCursor
import org.com.belog.group.service.result.ActiveMeetingResult
import org.com.belog.group.service.result.CreatedGroup
import org.com.belog.group.service.result.GroupDetailResult
import org.com.belog.group.service.result.GroupPreviewMemberResult
import org.com.belog.group.service.result.MyGroupListResult
import org.com.belog.group.service.result.MyGroupSummaryResult
import org.com.belog.group.service.result.SchedulingMeetingResult
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.user.service.UserService
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class GroupService(
    private val groupCreationAttemptService: GroupCreationAttemptService,
    private val inviteCodeGenerator: RandomInviteCodeGenerator,
    private val inviteLinkGenerator: GroupInviteLinkGenerator,
    private val groupCoverImageObjectVerifier: S3GroupCoverImageObjectVerifier,
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingRepository: MeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val settlementRequestService: SettlementRequestService,
    private val s3ObjectReadUrlProvider: S3ObjectReadUrlProvider,
    private val userService: UserService,
    private val clock: Clock,
) {
    fun createGroup(
        creatorId: Long,
        name: String,
        coverImageObjectKey: GroupCoverImageObjectKey?,
    ): CreatedGroup {
        coverImageObjectKey?.let(groupCoverImageObjectVerifier::verify)

        repeat(MAX_INVITE_CODE_ATTEMPTS) {
            val inviteCode = inviteCodeGenerator.generate()

            try {
                val group =
                    groupCreationAttemptService.create(
                        creatorId = creatorId,
                        name = name,
                        coverImageObjectKey = coverImageObjectKey,
                        inviteCode = inviteCode,
                    )

                return CreatedGroup(
                    groupId = requireNotNull(group.id),
                    name = group.name,
                    currentMemberCount = INITIAL_MEMBER_COUNT,
                    inviteCode = group.inviteCode,
                    inviteLink = inviteLinkGenerator.generate(inviteCode),
                )
            } catch (exception: DataIntegrityViolationException) {
                if (!exception.isInviteCodeUniqueConstraintViolation()) {
                    throw exception
                }
            }
        }

        throw BusinessException(GroupErrorCode.INVITE_CODE_ISSUANCE_FAILED)
    }

    @Transactional(readOnly = true)
    fun getMyGroups(
        userId: Long,
        cursor: GroupListCursor?,
        size: Int,
    ): MyGroupListResult {
        require(size in MIN_GROUP_LIST_PAGE_SIZE..MAX_GROUP_LIST_PAGE_SIZE) {
            "그룹 목록 조회 개수는 ${MIN_GROUP_LIST_PAGE_SIZE}개 이상 ${MAX_GROUP_LIST_PAGE_SIZE}개 이하여야 합니다."
        }

        val memberships =
            groupMemberRepository.findMyGroupPage(
                userId = userId,
                cursorPinned = cursor?.pinned,
                cursorId = cursor?.groupMemberId,
                pageable = PageRequest.of(0, size + NEXT_PAGE_LOOKAHEAD_COUNT),
            )
        val hasNext = memberships.size > size
        val pageMemberships = memberships.take(size)
        val membersByGroupId = findMembersByGroupId(pageMemberships)

        return MyGroupListResult(
            items =
                pageMemberships.map { membership ->
                    createMyGroupSummary(
                        membership = membership,
                        members = membersByGroupId.getValue(requireNotNull(membership.group.id)),
                    )
                },
            nextCursor =
                pageMemberships
                    .lastOrNull()
                    ?.takeIf { hasNext }
                    ?.let { membership ->
                        GroupListCursor(
                            pinned = membership.pinned,
                            groupMemberId = requireNotNull(membership.id),
                        )
                    },
            hasNext = hasNext,
        )
    }

    @Transactional
    fun pinGroup(
        groupId: Long,
        userId: Long,
    ) {
        findGroup(groupId)
        findCurrentMember(groupId, userId).pin()
    }

    @Transactional
    fun unpinGroup(
        groupId: Long,
        userId: Long,
    ) {
        findGroup(groupId)
        findCurrentMember(groupId, userId).unpin()
    }

    @Transactional
    fun deleteGroup(
        groupId: Long,
        userId: Long,
    ) {
        val group =
            groupRepository.findActiveByIdForUpdate(groupId)
                ?: throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
        if (findCurrentMember(groupId, userId).role != GroupRole.OWNER) {
            throw BusinessException(GroupErrorCode.GROUP_DELETE_OWNER_REQUIRED)
        }
        val meetings = meetingRepository.findAllByGroupIdForUpdate(groupId)
        settlementRequestService.validateGroupSettled(groupId)

        val deletedAt = Instant.now(clock)
        group.delete(deletedAt)
        meetings.forEach { meeting -> meeting.delete(deletedAt) }
    }

    @Transactional(readOnly = true)
    fun getGroup(
        groupId: Long,
        userId: Long,
    ): GroupDetailResult {
        val group = findGroup(groupId)
        val currentMember = findCurrentMember(groupId, userId)
        val currentDate = clock.currentBusinessDate()
        val schedulingMeetings = meetingRepository.findSchedulingMeetings(groupId, MeetingStatus.SCHEDULING)
        val activeMeetings = meetingRepository.findActiveMeetings(groupId, currentDate, MeetingStatus.CONFIRMED)
        val canManageGroup = currentMember.role == GroupRole.OWNER

        return GroupDetailResult(
            groupId = requireNotNull(group.id),
            name = group.name,
            coverImageUrl = group.coverImageObjectKey?.let(s3ObjectReadUrlProvider::generateReadUrl),
            inviteCode = group.inviteCode,
            memberCount = groupMemberRepository.countByGroupId(groupId).toInt(),
            canEditCoverImage = canManageGroup,
            canDeleteGroup = canManageGroup,
            schedulingMeetings = createSchedulingMeetingResults(schedulingMeetings),
            activeMeetings = activeMeetings.map(::createActiveMeetingResult),
        )
    }

    private fun findGroup(groupId: Long): Group =
        groupRepository.findById(groupId).orElseThrow {
            BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
        }

    private fun findCurrentMember(
        groupId: Long,
        userId: Long,
    ): GroupMember =
        groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)

    private fun findMembersByGroupId(memberships: List<GroupMember>): Map<Long, List<GroupMember>> {
        if (memberships.isEmpty()) {
            return emptyMap()
        }

        val groupIds = memberships.map { membership -> requireNotNull(membership.group.id) }
        return groupMemberRepository
            .findAllWithUserByGroupIdIn(groupIds)
            .groupBy { member -> requireNotNull(member.group.id) }
    }

    private fun createMyGroupSummary(
        membership: GroupMember,
        members: List<GroupMember>,
    ): MyGroupSummaryResult {
        val group = membership.group

        return MyGroupSummaryResult(
            groupId = requireNotNull(group.id),
            name = group.name,
            coverImageUrl = group.coverImageObjectKey?.let(s3ObjectReadUrlProvider::generateReadUrl),
            memberCount = members.size,
            previewMembers = members.take(PREVIEW_MEMBER_COUNT).map(::createGroupPreviewMemberResult),
            pinned = membership.pinned,
            canDeleteGroup = membership.role == GroupRole.OWNER,
        )
    }

    private fun createGroupPreviewMemberResult(member: GroupMember): GroupPreviewMemberResult {
        val user = member.user
        return GroupPreviewMemberResult(
            groupMemberId = requireNotNull(member.id),
            nickname = requireNotNull(user.nickname),
            profileImageUrl = userService.resolveProfileImageUrl(user),
        )
    }

    private fun createSchedulingMeetingResults(meetings: List<Meeting>): List<SchedulingMeetingResult> {
        if (meetings.isEmpty()) {
            return emptyList()
        }

        val meetingIds = meetings.map { meeting -> requireNotNull(meeting.id) }
        val participantsByMeetingId =
            meetingParticipantRepository
                .findAllWithUserByMeetingIdIn(meetingIds)
                .groupBy { participant -> requireNotNull(participant.meeting.id) }

        return meetings.map { meeting ->
            val meetingId = requireNotNull(meeting.id)
            val participants = participantsByMeetingId[meetingId].orEmpty()
            SchedulingMeetingResult(
                meetingId = meetingId,
                name = meeting.name,
                participantNicknames =
                    participants.map { participant ->
                        requireNotNull(participant.groupMember.user.nickname)
                    },
                participantCount = participants.size,
            )
        }
    }

    private fun createActiveMeetingResult(meeting: Meeting): ActiveMeetingResult =
        ActiveMeetingResult(
            meetingId = requireNotNull(meeting.id),
            name = meeting.name,
            startDate = requireNotNull(meeting.startDate),
            endDate = requireNotNull(meeting.endDate),
        )

    private fun DataIntegrityViolationException.isInviteCodeUniqueConstraintViolation(): Boolean {
        val constraintName =
            generateSequence(this as Throwable?) { throwable -> throwable.cause }
                .filterIsInstance<ConstraintViolationException>()
                .firstOrNull()
                ?.constraintName

        val unqualifiedConstraintName = constraintName?.substringAfterLast('.')?.trim('`', '"')
        return unqualifiedConstraintName.equals(GROUP_INVITE_CODE_UNIQUE_CONSTRAINT_NAME, ignoreCase = true)
    }

    companion object {
        const val MAX_INVITE_CODE_ATTEMPTS = 5
        private const val INITIAL_MEMBER_COUNT = 1
        private const val MIN_GROUP_LIST_PAGE_SIZE = 1
        private const val MAX_GROUP_LIST_PAGE_SIZE = 50
        private const val NEXT_PAGE_LOOKAHEAD_COUNT = 1
        private const val PREVIEW_MEMBER_COUNT = 3
    }
}
