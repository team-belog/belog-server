package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogDraft
import org.springframework.data.jpa.repository.JpaRepository

interface PostLogDraftRepository : JpaRepository<PostLogDraft, Long> {
    fun findByMeetingIdAndCreatedById(
        meetingId: Long,
        groupMemberId: Long,
    ): PostLogDraft?
}
