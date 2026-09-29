package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogPhotoLike
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface PostLogPhotoLikeRepository :
    JpaRepository<PostLogPhotoLike, Long>,
    PostLogPhotoLikeRepositoryCustom {
    fun countByPhotoId(photoId: Long): Long

    @Modifying
    @Query(
        """
        DELETE FROM PostLogPhotoLike photoLike
        WHERE photoLike.photo.id = :photoId
          AND photoLike.groupMember.id = :groupMemberId
        """,
    )
    fun deleteByPhotoIdAndGroupMemberId(
        @Param("photoId") photoId: Long,
        @Param("groupMemberId") groupMemberId: Long,
    ): Int
}
