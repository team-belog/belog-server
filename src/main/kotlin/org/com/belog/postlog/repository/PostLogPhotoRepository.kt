package org.com.belog.postlog.repository

import jakarta.persistence.LockModeType
import org.com.belog.postlog.domain.PostLogPhoto
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalDate

private const val FIND_ACTIVE_PHOTO_BY_ID_QUERY =
    """
    SELECT photo
    FROM PostLogPhoto photo
    WHERE photo.id = :photoId
      AND EXISTS (
        SELECT meeting.id
        FROM Meeting meeting
        WHERE meeting.id = photo.meeting.id
          AND meeting.deletedAt IS NULL
      )
    """

interface PostLogPhotoRepository : JpaRepository<PostLogPhoto, Long> {
    fun findAllByObjectKeyIn(objectKeys: Collection<String>): List<PostLogPhoto>

    fun existsByMeetingId(meetingId: Long): Boolean

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query(FIND_ACTIVE_PHOTO_BY_ID_QUERY)
    fun findByIdForShare(
        @Param("photoId") photoId: Long,
    ): PostLogPhoto?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(FIND_ACTIVE_PHOTO_BY_ID_QUERY)
    fun findByIdForUpdate(
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
        ORDER BY photo.capturedLocalDate ASC, photo.capturedAt ASC, photo.id ASC
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
            photo.capturedLocalDate > :capturedLocalDate
            OR (photo.capturedLocalDate = :capturedLocalDate AND photo.capturedAt > :capturedAt)
            OR (
              photo.capturedLocalDate = :capturedLocalDate
              AND photo.capturedAt = :capturedAt
              AND photo.id > :photoId
            )
          )
        ORDER BY photo.capturedLocalDate ASC, photo.capturedAt ASC, photo.id ASC
        """,
    )
    fun findPageAfter(
        @Param("meetingId") meetingId: Long,
        @Param("capturedLocalDate") capturedLocalDate: LocalDate,
        @Param("capturedAt") capturedAt: Instant,
        @Param("photoId") photoId: Long,
        pageable: Pageable,
    ): List<PostLogPhoto>
}
