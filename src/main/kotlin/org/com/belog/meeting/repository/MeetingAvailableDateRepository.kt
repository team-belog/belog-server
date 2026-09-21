package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.MeetingAvailableDate
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingAvailableDateRepository : JpaRepository<MeetingAvailableDate, Long>
