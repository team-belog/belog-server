package org.com.belog.group.service

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
import org.com.belog.group.service.result.ActiveMeetingResult
import org.com.belog.group.service.result.CreatedGroup
import org.com.belog.group.service.result.GroupDetailResult
import org.com.belog.group.service.result.SchedulingMeetingResult
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

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
    private val s3ObjectReadUrlProvider: S3ObjectReadUrlProvider,
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
    }
}
