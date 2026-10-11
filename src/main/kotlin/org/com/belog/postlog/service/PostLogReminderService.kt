package org.com.belog.postlog.service

import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.notification.service.NotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PostLogReminderService(
    private val meetingRepository: MeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val notificationService: NotificationService,
) {
    @Transactional(readOnly = true)
    fun findMeetingIdsEndingToday(
        currentDate: LocalDate,
        cursor: Long?,
        size: Int,
    ): List<Long> {
        require(size > 0) { "리마인드 대상 조회 개수는 1개 이상이어야 합니다." }

        return meetingRepository.findIdsByEndDate(
            endDate = currentDate,
            cursor = cursor,
            status = MeetingStatus.CONFIRMED,
            pageable = PageRequest.of(0, size),
        )
    }

    @Transactional
    fun remindPostLogOnMeetingEndDate(
        meetingId: Long,
        currentDate: LocalDate,
    ) {
        val meeting = meetingRepository.findActiveById(meetingId) ?: return
        if (!meeting.endsOn(currentDate)) {
            return
        }

        val commands =
            meetingParticipantRepository.findAllActiveWithUserByMeetingId(meetingId).map { participant ->
                PostLogNotificationCommands.postLogTodayReminder(
                    meetingId = meetingId,
                    endDate = currentDate,
                    recipientUserId = checkNotNull(participant.groupMember.user.id) { "만남 참여자의 사용자 ID가 없습니다." },
                )
            }
        notificationService.createAll(commands)
    }
}
