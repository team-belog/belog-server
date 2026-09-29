package org.com.belog.billlog.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun calculateBillDayNumber(
    meetingStartDate: LocalDate,
    paymentDate: LocalDate,
): Int {
    val daysFromMeetingStart = ChronoUnit.DAYS.between(meetingStartDate, paymentDate)
    val dayNumber = if (daysFromMeetingStart < 0) daysFromMeetingStart else daysFromMeetingStart + 1
    return Math.toIntExact(dayNumber)
}
