package org.com.belog.auth.repository

import org.com.belog.auth.domain.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByUserId(userId: Long): RefreshToken?
}
