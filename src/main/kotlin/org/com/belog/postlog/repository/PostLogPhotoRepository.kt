package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogPhoto
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface PostLogPhotoRepository : JpaRepository<PostLogPhoto, Long> {
    fun findAllByObjectKeyIn(objectKeys: Collection<String>): List<PostLogPhoto>

    fun existsByMeetingId(meetingId: Long): Boolean

    @Query(
        """
        SELECT photo
        FROM PostLogPhoto photo
        JOIN FETCH photo.meeting meeting
        JOIN FETCH meeting.group
        WHERE photo.id = :photoId
          AND meeting.deletedAt IS NULL
        """,
    )
    fun findByIdWithMeeting(
        @Param("photoId") photoId: Long,
    ): PostLogPhoto?

    @Query(
        """
        SELECT photo.objectKey
        FROM PostLogPhoto photo
        LEFT JOIN PostLogPhotoLike photoLike ON photoLike.photo = photo
        WHERE photo.meeting.id = :meetingId
        GROUP BY photo.id, photo.objectKey
        ORDER BY COUNT(photoLike.id) DESC, photo.id ASC
        """,
    )
    fun findRepresentativeObjectKeys(
        @Param("meetingId") meetingId: Long,
        pageable: Pageable,
    ): List<String>

    @Query(
        """
        SELECT photo
        FROM PostLogPhoto photo
        WHERE photo.meeting.id = :meetingId
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
        WHERE photo.meeting.id = :meetingId
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
