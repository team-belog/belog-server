package org.com.belog.prelog.service

import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.notification.service.NotificationService
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class PreLogReminderService(
    private val meetingRepository: MeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val notificationService: NotificationService,
) {
    @Transactional(readOnly = true)
    fun findMeetingIdsStartingInTwoDays(
        currentDate: LocalDate,
        cursor: Long?,
        size: Int,
    ): List<Long> {
        require(size > 0) { "리마인드 대상 조회 개수는 1개 이상이어야 합니다." }

        return meetingRepository.findIdsByStartDate(
            startDate = currentDate.plusDays(PRE_LOG_REMINDER_DAYS_BEFORE_START),
            cursor = cursor,
            status = MeetingStatus.CONFIRMED,
            pageable = PageRequest.of(0, size),
        )
    }

    @Transactional
    fun remindPreLogBeforeMeetingStart(
        meetingId: Long,
        currentDate: LocalDate,
    ) {
        val startDate = currentDate.plusDays(PRE_LOG_REMINDER_DAYS_BEFORE_START)
        val meeting = meetingRepository.findActiveById(meetingId) ?: return
        if (!meeting.startsOn(startDate)) {
            return
        }

        val commands =
            meetingParticipantRepository.findAllActiveWithUserByMeetingId(meetingId).map { participant ->
                PreLogNotificationCommands.preLogD2Reminder(
                    meetingId = meetingId,
                    startDate = startDate,
                    recipientUserId = checkNotNull(participant.groupMember.user.id) { "만남 참여자의 사용자 ID가 없습니다." },
                )
            }
        notificationService.createAll(commands)
    }

    companion object {
        private const val PRE_LOG_REMINDER_DAYS_BEFORE_START = 2L
    }
}
