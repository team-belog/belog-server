package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.Meeting
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingRepository : JpaRepository<Meeting, Long>
