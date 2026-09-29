package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogPhoto
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

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

    @Query(
        """
        SELECT photo
        FROM PostLogPhoto photo
        WHERE photo.postLog.meeting.id = :meetingId
        ORDER BY photo.capturedAt ASC, photo.id ASC
        """,
    )
    fun findPage(
        @Param("meetingId") meetingId: Long,
        pageable: Pageable,
    ): List<PostLogPhoto>

    @Query(
        """
        SELECT photo
        FROM PostLogPhoto photo
        WHERE photo.postLog.meeting.id = :meetingId
          AND (
            photo.capturedAt > :capturedAt
            OR (photo.capturedAt = :capturedAt AND photo.id > :photoId)
          )
        ORDER BY photo.capturedAt ASC, photo.id ASC
        """,
    )
    fun findPageAfter(
        @Param("meetingId") meetingId: Long,
        @Param("capturedAt") capturedAt: Instant,
        @Param("photoId") photoId: Long,
        pageable: Pageable,
    ): List<PostLogPhoto>
}
