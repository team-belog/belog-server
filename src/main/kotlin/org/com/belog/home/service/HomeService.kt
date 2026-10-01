package org.com.belog.home.service

import org.com.belog.global.time.currentBusinessDate
import org.com.belog.home.repository.HomeCalendarRepository
import org.com.belog.home.service.result.HomeCalendarMeetingResult
import org.com.belog.home.service.result.HomeCalendarResult
import org.com.belog.meeting.domain.MeetingStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.YearMonth

@Service
class HomeService(
    private val homeCalendarRepository: HomeCalendarRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getCalendar(
        userId: Long,
        yearMonth: YearMonth,
    ): HomeCalendarResult {
        val meetings =
            homeCalendarRepository.findMeetingsOverlappingMonth(
                userId = userId,
                status = MeetingStatus.CONFIRMED,
                monthStart = yearMonth.atDay(1),
                monthEnd = yearMonth.atEndOfMonth(),
            )

        return HomeCalendarResult(
            yearMonth = yearMonth,
            today = clock.currentBusinessDate(),
            meetings =
                meetings.map { meeting ->
                    HomeCalendarMeetingResult(
                        meetingId = requireNotNull(meeting.id),
                        startDate = requireNotNull(meeting.startDate),
                        endDate = requireNotNull(meeting.endDate),
                    )
                },
        )
    }
}
