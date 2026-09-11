package org.com.belog.auth.repository

import jakarta.persistence.LockModeType
import org.com.belog.auth.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByUserId(userId: Long): RefreshToken?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select refreshToken from RefreshToken refreshToken where refreshToken.user.id = :userId")
    fun findByUserIdForUpdate(
        @Param("userId") userId: Long,
    ): RefreshToken?
}
