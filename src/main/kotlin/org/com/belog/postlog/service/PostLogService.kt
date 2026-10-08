package org.com.belog.postlog.service

import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.POST_LOG_TICKET_MEETING_CREATOR_UNIQUE_CONSTRAINT_NAME
import org.com.belog.postlog.domain.PostLogDraft
import org.com.belog.postlog.domain.PostLogTicket
import org.com.belog.postlog.repository.PostLogDraftRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogTicketRepository
import org.com.belog.postlog.service.result.PostLogParticipantResult
import org.com.belog.postlog.service.result.PostLogSummaryResult
import org.com.belog.postlog.service.result.PostLogTicketMemberResult
import org.com.belog.postlog.service.result.PostLogTicketResult
import org.com.belog.user.service.UserService
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class PostLogService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val postLogDraftRepository: PostLogDraftRepository,
    private val postLogTicketRepository: PostLogTicketRepository,
    private val postLogPhotoRepository: PostLogPhotoRepository,
    private val billRepository: BillRepository,
    private val settlementRequestRepository: SettlementRequestRepository,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
    private val userService: UserService,
    private val clock: Clock,
) {
    @Transactional
    fun saveDraft(
        meetingId: Long,
        userId: Long,
        memory: String,
    ) {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val creator =
            meetingParticipantRepository
                .findByMeetingIdAndUserIdForUpdate(meetingId, userId)
                ?.groupMember
                ?: throw BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT)
        val creatorId = checkNotNull(creator.id) { "Post-log 임시저장 작성자의 그룹 멤버 ID가 없습니다." }
        if (postLogTicketRepository.existsBySourceMeetingIdAndCreatorGroupMemberId(meetingId, creatorId)) {
            throw BusinessException(PostLogErrorCode.TICKET_ALREADY_CREATED)
        }

        val draft = postLogDraftRepository.findByMeetingIdAndCreatedById(meetingId, creatorId)
        if (draft == null) {
            postLogDraftRepository.save(PostLogDraft.create(meeting, creator, memory))
        } else {
            draft.updateMemory(memory)
        }
    }

    @Transactional
    fun createTicket(
        meetingId: Long,
        userId: Long,
        memory: String,
    ): PostLogTicketResult {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val creator =
            meetingParticipantRepository
                .findByMeetingIdAndUserIdForUpdate(meetingId, userId)
                ?.groupMember
                ?: throw BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT)
        val creatorId = checkNotNull(creator.id) { "티켓 생성자의 그룹 멤버 ID가 없습니다." }
        if (postLogTicketRepository.existsBySourceMeetingIdAndCreatorGroupMemberId(meetingId, creatorId)) {
            throw BusinessException(PostLogErrorCode.TICKET_ALREADY_CREATED)
        }

        val ticket =
            PostLogTicket.issue(
                meeting = meeting,
                creator = creator,
                memory = memory,
                coverImageObjectKey = findRepresentativeObjectKey(meetingId),
                issuedAt = Instant.now(clock),
            )
        val savedTicket =
            try {
                postLogTicketRepository.saveAndFlush(ticket)
            } catch (exception: DataIntegrityViolationException) {
                if (exception.isMeetingCreatorUniqueConstraintViolation()) {
                    throw BusinessException(PostLogErrorCode.TICKET_ALREADY_CREATED, exception)
                }
                throw exception
            }
        postLogDraftRepository.findByMeetingIdAndCreatedById(meetingId, creatorId)?.let(postLogDraftRepository::delete)
        return toTicketResult(savedTicket)
    }

    @Transactional(readOnly = true)
    fun getTicket(
        ticketId: Long,
        userId: Long,
    ): PostLogTicketResult {
        val ticket =
            postLogTicketRepository.findByIdAndOwnerId(ticketId, userId)
                ?: throw BusinessException(PostLogErrorCode.TICKET_NOT_FOUND)

        return toTicketResult(ticket)
    }

    @Transactional(readOnly = true)
    fun getSummary(
        meetingId: Long,
        userId: Long,
    ): PostLogSummaryResult {
        val meeting =
            meetingRepository.findByIdWithGroupOwnerAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val viewer =
            groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val viewerId = checkNotNull(viewer.id) { "조회자의 그룹 멤버 ID가 없습니다." }

        val participants =
            meetingParticipantRepository
                .findAllWithMemberAndUserByMeetingId(meetingId)
                .map { participant ->
                    val groupMember = participant.groupMember
                    PostLogParticipantResult(
                        groupMemberId = checkNotNull(groupMember.id) { "조회된 그룹 멤버의 ID가 없습니다." },
                        nickname = userService.resolveDisplayNickname(groupMember.user),
                        meetingCreator = meeting.isCreatedBy(groupMember),
                    )
                }
        val ticket = postLogTicketRepository.findBySourceMeetingIdAndCreatorGroupMemberId(meetingId, viewerId)
        val draftMemory =
            if (ticket == null) {
                postLogDraftRepository.findByMeetingIdAndCreatedById(meetingId, viewerId)?.memory
            } else {
                null
            }

        return PostLogSummaryResult(
            ticketId = ticket?.id,
            meetingId = checkNotNull(meeting.id) { "조회된 만남의 ID가 없습니다." },
            meetingName = meeting.name,
            startDate = meeting.startDate,
            endDate = meeting.endDate,
            location = meeting.location,
            memory = ticket?.memory ?: draftMemory,
            totalAmount = billRepository.sumTotalAmountByMeetingId(meetingId),
            completedParticipantCount =
                settlementRequestRepository.countCompletedParticipantsByMeetingId(
                    meetingId = meetingId,
                    pendingStatus = SettlementRequestStatus.PENDING,
                ),
            participants = participants,
            ticketCreated = ticket != null,
        )
    }

    companion object {
        private const val REPRESENTATIVE_PHOTO_COUNT = 1
    }

    private fun findRepresentativeObjectKey(meetingId: Long): String? =
        postLogPhotoRepository
            .findRepresentativeObjectKeys(meetingId, PageRequest.of(0, REPRESENTATIVE_PHOTO_COUNT))
            .firstOrNull()

    private fun toTicketResult(ticket: PostLogTicket): PostLogTicketResult {
        val members =
            meetingParticipantRepository
                .findAllWithUserByMeetingIdIn(listOf(ticket.sourceMeetingId))
                .map { participant ->
                    val groupMember = participant.groupMember
                    PostLogTicketMemberResult(
                        groupMemberId = checkNotNull(groupMember.id) { "조회된 그룹 멤버의 ID가 없습니다." },
                        nickname = userService.resolveDisplayNickname(groupMember.user),
                    )
                }

        return PostLogTicketResult(
            ticketId = checkNotNull(ticket.id) { "Post-log 티켓의 ID가 없습니다." },
            meetingName = ticket.meetingName,
            memory = ticket.memory,
            coverPhotoUrl = ticket.coverImageObjectKey?.let(objectReadUrlProvider::generateReadUrl),
            startDate = ticket.meetingStartDate,
            endDate = ticket.meetingEndDate,
            location = ticket.meetingLocation,
            members = members,
        )
    }

    private fun DataIntegrityViolationException.isMeetingCreatorUniqueConstraintViolation(): Boolean {
        val constraintName =
            generateSequence(this as Throwable?) { throwable -> throwable.cause }
                .filterIsInstance<ConstraintViolationException>()
                .firstOrNull()
                ?.constraintName

        val unqualifiedConstraintName = constraintName?.substringAfterLast('.')?.trim('`', '"')
        return unqualifiedConstraintName.equals(POST_LOG_TICKET_MEETING_CREATOR_UNIQUE_CONSTRAINT_NAME, ignoreCase = true)
    }
}
