package org.com.belog.postlog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.group.domain.GroupMember
import org.com.belog.meeting.domain.Meeting
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

const val POST_LOG_PHOTO_OBJECT_KEY_UNIQUE_CONSTRAINT_NAME = "uk_post_log_photos_object_key"

@Entity
@Table(
    name = "post_log_photos",
    uniqueConstraints = [
        UniqueConstraint(
            name = POST_LOG_PHOTO_OBJECT_KEY_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["object_key"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_post_log_photos_meeting_captured_id",
            columnList = "meeting_id, captured_at_utc, id",
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_post_log_photos_captured_offset",
            constraint = "captured_offset_minutes BETWEEN -1080 AND 1080",
        ),
    ],
)
class PostLogPhoto protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_photos_meeting_id"),
    )
    val meeting: Meeting,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "uploaded_by_group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_photos_uploaded_by_group_member_id"),
    )
    val uploadedBy: GroupMember,
    @Column(name = "object_key", nullable = false, length = PostLogPhotoObjectKey.MAX_LENGTH)
    val objectKey: String,
    @Column(name = "captured_at_utc", nullable = false)
    val capturedAt: Instant,
    @Column(name = "captured_offset_minutes", nullable = false)
    val capturedOffsetMinutes: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    fun capturedAtWithOffset(): OffsetDateTime =
        capturedAt.atOffset(
            ZoneOffset.ofTotalSeconds(capturedOffsetMinutes * SECONDS_PER_MINUTE),
        )

    companion object {
        const val MAX_UPLOAD_COUNT = 100

        fun create(
            meeting: Meeting,
            uploader: GroupMember,
            objectKey: PostLogPhotoObjectKey,
            capturedAt: OffsetDateTime,
        ): PostLogPhoto {
            require(uploader.belongsTo(meeting.group)) {
                "Post-log 사진 업로더는 해당 만남이 속한 그룹의 멤버여야 합니다."
            }
            val offsetTotalSeconds = capturedAt.offset.totalSeconds
            require(offsetTotalSeconds % SECONDS_PER_MINUTE == 0) {
                "Post-log 사진 촬영 시각의 UTC 오프셋은 분 단위여야 합니다."
            }

            return PostLogPhoto(
                meeting = meeting,
                uploadedBy = uploader,
                objectKey = objectKey.value,
                capturedAt = capturedAt.toInstant().truncatedTo(ChronoUnit.MICROS),
                capturedOffsetMinutes = offsetTotalSeconds / SECONDS_PER_MINUTE,
            )
        }

        internal const val SECONDS_PER_MINUTE = 60
    }
}
