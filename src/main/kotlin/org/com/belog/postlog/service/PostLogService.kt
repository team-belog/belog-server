package org.com.belog.postlog.service

import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.POST_LOG_MEETING_MEMBER_UNIQUE_CONSTRAINT_NAME
import org.com.belog.postlog.domain.PostLog
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogRepository
import org.com.belog.postlog.service.result.PostLogParticipantResult
import org.com.belog.postlog.service.result.PostLogSummaryResult
import org.com.belog.postlog.service.result.PostLogTicketMemberResult
import org.com.belog.postlog.service.result.PostLogTicketResult
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
    private val postLogRepository: PostLogRepository,
    private val postLogPhotoRepository: PostLogPhotoRepository,
    private val billRepository: BillRepository,
    private val settlementRequestRepository: SettlementRequestRepository,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
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
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val creator =
            groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val creatorId = checkNotNull(creator.id) { "Post-log 임시저장 작성자의 그룹 멤버 ID가 없습니다." }

        val postLog =
            postLogRepository.findByMeetingIdAndCreatedById(meetingId, creatorId)
                ?: PostLog.create(meeting, creator)
        if (postLog.isTicketCreated) {
            throw BusinessException(PostLogErrorCode.TICKET_ALREADY_CREATED)
        }

        postLog.updateMemory(memory)
        postLogRepository.save(postLog)
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
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val creator =
            groupMemberRepository.findByGroupIdAndUserIdForUpdate(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val creatorId = checkNotNull(creator.id) { "티켓 생성자의 그룹 멤버 ID가 없습니다." }

        val postLog =
            postLogRepository.findByMeetingIdAndCreatedById(meetingId, creatorId)
                ?: PostLog.create(meeting, creator)
        if (postLog.isTicketCreated) {
            throw BusinessException(PostLogErrorCode.TICKET_ALREADY_CREATED)
        }

        postLog.updateMemory(memory)
        check(postLog.createTicket(Instant.now(clock))) { "티켓 생성 상태를 변경할 수 없습니다." }

        val savedPostLog =
            try {
                postLogRepository.saveAndFlush(postLog)
            } catch (exception: DataIntegrityViolationException) {
                if (exception.isMeetingMemberUniqueConstraintViolation()) {
                    throw BusinessException(PostLogErrorCode.TICKET_ALREADY_CREATED, exception)
                }
                throw exception
            }
        return toTicketResult(meeting, savedPostLog)
    }

    @Transactional(readOnly = true)
    fun getTicket(
        meetingId: Long,
        userId: Long,
    ): PostLogTicketResult {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val viewer =
            groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val viewerId = checkNotNull(viewer.id) { "티켓 조회자의 그룹 멤버 ID가 없습니다." }
        val postLog =
            postLogRepository
                .findByMeetingIdAndCreatedById(meetingId, viewerId)
                ?.takeIf(PostLog::isTicketCreated)
                ?: throw BusinessException(PostLogErrorCode.TICKET_NOT_FOUND)

        return toTicketResult(meeting, postLog)
    }

    @Transactional(readOnly = true)
    fun getSummary(
        meetingId: Long,
        userId: Long,
    ): PostLogSummaryResult {
        val meeting =
            meetingRepository.findByIdWithGroupAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        val viewer =
            groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val viewerId = checkNotNull(viewer.id) { "조회자의 그룹 멤버 ID가 없습니다." }

        val participants =
            meetingParticipantRepository
                .findAllWithMemberAndUserByMeetingId(meetingId)
                .map { participant ->
                    val groupMember = participant.groupMember
                    PostLogParticipantResult(
                        groupMemberId = checkNotNull(groupMember.id) { "조회된 그룹 멤버의 ID가 없습니다." },
                        nickname = checkNotNull(groupMember.user.nickname) { "조회된 참여자의 닉네임이 없습니다." },
                        meetingCreator = meeting.isCreatedBy(groupMember),
                    )
                }
        val postLog = postLogRepository.findByMeetingIdAndCreatedById(meetingId, viewerId)

        return PostLogSummaryResult(
            postLogId = postLog?.id,
            meetingId = checkNotNull(meeting.id) { "조회된 만남의 ID가 없습니다." },
            meetingName = meeting.name,
            startDate = meeting.startDate,
            endDate = meeting.endDate,
            location = meeting.location,
            memory = postLog?.memory,
            totalAmount = billRepository.sumTotalAmountByMeetingId(meetingId),
            completedParticipantCount =
                settlementRequestRepository.countCompletedParticipantsByMeetingId(
                    meetingId = meetingId,
                    pendingStatus = SettlementRequestStatus.PENDING,
                ),
            participants = participants,
            ticketCreated = postLog?.isTicketCreated == true,
        )
    }

    companion object {
        private const val REPRESENTATIVE_PHOTO_COUNT = 1
    }

    private fun toTicketResult(
        meeting: Meeting,
        postLog: PostLog,
    ): PostLogTicketResult {
        val meetingId = checkNotNull(meeting.id) { "티켓 대상 만남의 ID가 없습니다." }
        val members =
            meetingParticipantRepository
                .findAllWithMemberAndUserByMeetingId(meetingId)
                .map { participant ->
                    val groupMember = participant.groupMember
                    PostLogTicketMemberResult(
                        groupMemberId = checkNotNull(groupMember.id) { "조회된 그룹 멤버의 ID가 없습니다." },
                        nickname = checkNotNull(groupMember.user.nickname) { "조회된 참여자의 닉네임이 없습니다." },
                    )
                }
        val coverPhotoUrl =
            postLogPhotoRepository
                .findRepresentativeObjectKeys(meetingId, PageRequest.of(0, REPRESENTATIVE_PHOTO_COUNT))
                .firstOrNull()
                ?.let(objectReadUrlProvider::generateReadUrl)

        return PostLogTicketResult(
            postLogId = checkNotNull(postLog.id) { "Post-log 티켓의 ID가 없습니다." },
            meetingId = meetingId,
            meetingName = meeting.name,
            memory = checkNotNull(postLog.memory) { "티켓의 추억 문구가 없습니다." },
            coverPhotoUrl = coverPhotoUrl,
            startDate = meeting.startDate,
            endDate = meeting.endDate,
            location = meeting.location,
            members = members,
        )
    }

    private fun DataIntegrityViolationException.isMeetingMemberUniqueConstraintViolation(): Boolean {
        val constraintName =
            generateSequence(this as Throwable?) { throwable -> throwable.cause }
                .filterIsInstance<ConstraintViolationException>()
                .firstOrNull()
                ?.constraintName

        val unqualifiedConstraintName = constraintName?.substringAfterLast('.')?.trim('`', '"')
        return unqualifiedConstraintName.equals(POST_LOG_MEETING_MEMBER_UNIQUE_CONSTRAINT_NAME, ignoreCase = true)
    }
}
