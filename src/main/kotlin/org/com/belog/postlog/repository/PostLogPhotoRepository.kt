package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogPhoto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLogPhotoRepository : JpaRepository<PostLogPhoto, Long> {
    fun findAllByObjectKeyIn(objectKeys: Collection<String>): List<PostLogPhoto>

    @Query(
        """
        SELECT photo
        FROM PostLogPhoto photo
        WHERE photo.id = :photoId
          AND photo.postLog.meeting.id = :meetingId
        """,
    )
    fun findByIdAndMeetingId(
        @Param("photoId") photoId: Long,
        @Param("meetingId") meetingId: Long,
    ): PostLogPhoto?
}
