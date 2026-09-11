package org.com.belog.user.repository

import jakarta.persistence.LockModeType
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): User?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT user 
        FROM User user 
        WHERE user.id = :userId
        """,
    )
    fun findByIdForUpdate(
        @Param("userId") userId: Long,
    ): User?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT user
        FROM User user
        WHERE user.provider = :provider
          AND user.providerUserId = :providerUserId
        """,
    )
    fun findByProviderAndProviderUserIdForUpdate(
        provider: SocialProvider,
        providerUserId: String,
    ): User?

    // 중복 경로는 현재 트랜잭션 연결의 LAST_INSERT_ID를 0으로 설정해 실제 INSERT와 구분한다.
    @Modifying
    @Query(
        value =
            """
            INSERT INTO users (
                email,
                nickname,
                profile_image_url,
                provider,
                provider_user_id,
                created_at,
                updated_at
            ) VALUES (
                :email,
                :nickname,
                :profileImageUrl,
                :provider,
                :providerUserId,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE id = id + LAST_INSERT_ID(0)
            """,
        nativeQuery = true,
    )
    fun upsertSocialUser(
        @Param("email") email: String,
        @Param("nickname") nickname: String?,
        @Param("profileImageUrl") profileImageUrl: String?,
        @Param("provider") provider: String,
        @Param("providerUserId") providerUserId: String,
    ): Int

    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    fun findLastInsertId(): Long
}
