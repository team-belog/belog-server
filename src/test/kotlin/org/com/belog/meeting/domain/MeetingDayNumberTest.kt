package org.com.belog.meeting.domain

import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class MeetingDayNumberTest {
    @Test
    fun `만남 시작 전 일차에는 -, 시작일 이후 일차에는 +를 붙인다`() {
        val meetingStartDate = LocalDate.of(2026, 9, 28)

        val signedDayNumbers =
            listOf(meetingStartDate.minusDays(1), meetingStartDate, meetingStartDate.plusDays(1))
                .map { date -> formatSignedMeetingDayNumber(calculateMeetingDayNumber(meetingStartDate, date)) }

        assertEquals(listOf("-1", "+1", "+2"), signedDayNumbers)
    }
}
