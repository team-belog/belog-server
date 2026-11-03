package org.com.belog.postlog.service

import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.notification.service.NotificationQueryService
import org.com.belog.postlog.repository.PostLogTicketRepository
import org.com.belog.postlog.service.result.PostLogTicketCalendarItemResult
import org.com.belog.postlog.service.result.PostLogTicketCalendarResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth

@Service
class PostLogCalendarService(
    private val postLogTicketRepository: PostLogTicketRepository,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
    private val notificationQueryService: NotificationQueryService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getMyTicketCalendar(
        userId: Long,
        yearMonth: YearMonth,
    ): PostLogTicketCalendarResult {
        val tickets =
            postLogTicketRepository
                .findCalendar(
                    userId = userId,
                    startDate = yearMonth.atDay(1),
                    endDate = yearMonth.atEndOfMonth(),
                ).map { ticket ->
                    PostLogTicketCalendarItemResult(
                        ticketId = requireNotNull(ticket.id),
                        meetingEndDate = requireNotNull(ticket.meetingEndDate),
                        thumbnailUrl = ticket.coverImageObjectKey?.let(objectReadUrlProvider::generateReadUrl),
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
}
