package org.com.belog.meeting.repository

import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.springframework.data.jpa.repository.JpaRepository

interface MeetingCandidateDateRangeRepository : JpaRepository<MeetingCandidateDateRange, Long>
