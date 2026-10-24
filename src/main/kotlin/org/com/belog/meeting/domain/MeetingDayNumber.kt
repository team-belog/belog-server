package org.com.belog.meeting.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

fun calculateMeetingDayNumber(
    meetingStartDate: LocalDate,
    date: LocalDate,
): Int {
    val daysFromMeetingStart = ChronoUnit.DAYS.between(meetingStartDate, date)
    val dayNumber = if (daysFromMeetingStart < 0) daysFromMeetingStart else daysFromMeetingStart + 1
    return Math.toIntExact(dayNumber)
}
