package org.com.belog.postlog.service

import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.notification.service.NotificationQueryService
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogRepository
import org.com.belog.postlog.service.result.PostLogTicketCalendarItemResult
import org.com.belog.postlog.service.result.PostLogTicketCalendarResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth

@Service
class PostLogCalendarService(
    private val postLogRepository: PostLogRepository,
    private val postLogPhotoRepository: PostLogPhotoRepository,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
    private val notificationQueryService: NotificationQueryService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getMyTicketCalendar(
        userId: Long,
        yearMonth: YearMonth,
    ): PostLogTicketCalendarResult {
        val postLogs =
            postLogRepository.findTicketCalendar(
                userId = userId,
                startDate = yearMonth.atDay(1),
                endDate = yearMonth.atEndOfMonth(),
            )
        val representativePhotoObjectKeys = findRepresentativePhotoObjectKeys(postLogs.map { requireNotNull(it.meeting.id) })
        val tickets =
            postLogs.map { postLog ->
                val meetingId = requireNotNull(postLog.meeting.id)
                PostLogTicketCalendarItemResult(
                    postLogId = requireNotNull(postLog.id),
                    meetingEndDate = requireNotNull(postLog.meeting.endDate),
                    thumbnailUrl = representativePhotoObjectKeys[meetingId]?.let(objectReadUrlProvider::generateReadUrl),
                )
            }

        return PostLogTicketCalendarResult(
            yearMonth = yearMonth,
            today = clock.currentBusinessDate(),
            hasUnreadNotification = notificationQueryService.hasUnreadNotification(userId),
            memoryCount = tickets.size,
            tickets = tickets,
        )
    }

    private fun findRepresentativePhotoObjectKeys(meetingIds: List<Long>): Map<Long, String> {
        if (meetingIds.isEmpty()) {
            return emptyMap()
        }

        val objectKeysByMeetingId = linkedMapOf<Long, String>()
        postLogPhotoRepository.findRepresentativePhotoCandidates(meetingIds).forEach { photo ->
            objectKeysByMeetingId.putIfAbsent(photo.meetingId, photo.objectKey)
        }
        return objectKeysByMeetingId
    }
}
