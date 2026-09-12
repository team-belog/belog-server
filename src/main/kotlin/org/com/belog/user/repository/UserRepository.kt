package org.com.belog.user.repository

import jakarta.persistence.LockModeType
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository :
    JpaRepository<User, Long>,
    UserRepositoryCustom {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): User?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT user
        FROM User user
        WHERE user.id = :userId
        """,
    )
    fun findByIdForUpdate(
        @Param("userId") userId: Long,
    ): User?
}
