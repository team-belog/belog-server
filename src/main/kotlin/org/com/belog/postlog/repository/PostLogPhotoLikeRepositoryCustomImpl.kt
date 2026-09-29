package org.com.belog.postlog.repository

import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

open class PostLogPhotoLikeRepositoryCustomImpl(
    private val entityManager: EntityManager,
) : PostLogPhotoLikeRepositoryCustom {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun saveIfAbsent(
        photoId: Long,
        groupMemberId: Long,
    ) {
        entityManager
            .createNativeQuery(SAVE_IF_ABSENT_SQL)
            .setParameter("photoId", photoId)
            .setParameter("groupMemberId", groupMemberId)
            .executeUpdate()
    }

    companion object {
        private const val SAVE_IF_ABSENT_SQL =
            """
            INSERT INTO post_log_photo_likes (
                photo_id,
                group_member_id,
                created_at,
                updated_at
            ) VALUES (
                :photoId,
                :groupMemberId,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE id = id
            """
    }
}
