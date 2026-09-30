package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLog
import org.springframework.data.jpa.repository.JpaRepository

interface PostLogRepository : JpaRepository<PostLog, Long> {
    fun findByMeetingId(meetingId: Long): PostLog?
}
