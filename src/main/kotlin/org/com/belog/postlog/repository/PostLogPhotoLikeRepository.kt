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

    @Query(
        """
        SELECT new org.com.belog.postlog.repository.PostLogPhotoLikeCount(
            photoLike.photo.id,
            COUNT(photoLike.id)
        )
        FROM PostLogPhotoLike photoLike
        WHERE photoLike.photo.id IN :photoIds
        GROUP BY photoLike.photo.id
        """,
    )
    fun countByPhotoIds(
        @Param("photoIds") photoIds: Collection<Long>,
    ): List<PostLogPhotoLikeCount>

    @Query(
        """
        SELECT photoLike.photo.id
        FROM PostLogPhotoLike photoLike
        WHERE photoLike.photo.id IN :photoIds
          AND photoLike.groupMember.id = :groupMemberId
        """,
    )
    fun findLikedPhotoIds(
        @Param("photoIds") photoIds: Collection<Long>,
        @Param("groupMemberId") groupMemberId: Long,
    ): List<Long>

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
