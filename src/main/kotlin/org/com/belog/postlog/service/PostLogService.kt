package org.com.belog.postlog.service

import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.repository.PostLogRepository
import org.com.belog.postlog.service.result.PostLogParticipantResult
import org.com.belog.postlog.service.result.PostLogSummaryResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostLogService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val postLogRepository: PostLogRepository,
    private val billRepository: BillRepository,
    private val settlementRequestRepository: SettlementRequestRepository,
) {
    @Transactional(readOnly = true)
    fun getSummary(
        meetingId: Long,
        userId: Long,
    ): PostLogSummaryResult {
        val meeting =
            meetingRepository.findByIdWithGroupAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Post-log 대상 만남의 그룹 ID가 없습니다." }
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

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
        val postLog = postLogRepository.findByMeetingId(meetingId)

        return PostLogSummaryResult(
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
}
