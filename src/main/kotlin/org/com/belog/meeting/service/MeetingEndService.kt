package org.com.belog.meeting.service

import org.com.belog.billlog.repository.BillRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.prelog.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MeetingEndService(
    private val meetingRepository: MeetingRepository,
    private val planRepository: PlanRepository,
    private val billRepository: BillRepository,
    private val postLogPhotoRepository: PostLogPhotoRepository,
    private val clock: Clock,
) {
    @Transactional
    fun endMeeting(
        meetingId: Long,
        userId: Long,
    ) {
        val meeting =
            meetingRepository.findByIdForUpdate(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        if (meeting.owner.user.id != userId) {
            throw BusinessException(MeetingErrorCode.NOT_MEETING_OWNER)
        }
        if (meeting.isEnded()) {
            throw BusinessException(MeetingErrorCode.MEETING_END_ALREADY_ENDED)
        }

        val currentDate = clock.currentBusinessDate()
        if (!meeting.hasStarted(currentDate)) {
            throw BusinessException(MeetingErrorCode.MEETING_END_NOT_STARTED)
        }
        if (!hasAllLogs(meetingId)) {
            throw BusinessException(MeetingErrorCode.MEETING_END_LOGS_INCOMPLETE)
        }

        meeting.end(endedAt = Instant.now(clock), currentDate = currentDate)
    }

    private fun hasAllLogs(meetingId: Long): Boolean =
        planRepository.existsByMeetingId(meetingId) &&
            billRepository.existsByMeetingId(meetingId) &&
            postLogPhotoRepository.existsByMeetingId(meetingId)
}
