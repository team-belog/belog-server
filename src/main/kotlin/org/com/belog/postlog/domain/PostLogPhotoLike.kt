package org.com.belog.postlog.domain

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

const val POST_LOG_PHOTO_LIKE_UNIQUE_CONSTRAINT_NAME = "uk_post_log_photo_likes_photo_member"

@Entity
@Table(
    name = "post_log_photo_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = POST_LOG_PHOTO_LIKE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["photo_id", "group_member_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_post_log_photo_likes_group_member_photo",
            columnList = "group_member_id, photo_id",
        ),
    ],
)
class PostLogPhotoLike protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "photo_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_photo_likes_photo_id"),
    )
    val photo: PostLogPhoto,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_post_log_photo_likes_group_member_id"),
    )
    val groupMember: GroupMember,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            photo: PostLogPhoto,
            groupMember: GroupMember,
        ): PostLogPhotoLike {
            require(groupMember.belongsTo(photo.postLog.meeting.group)) {
                "좋아요를 등록하는 사용자는 사진이 속한 그룹의 멤버여야 합니다."
            }

            return PostLogPhotoLike(
                photo = photo,
                groupMember = groupMember,
            )
        }
    }
}
