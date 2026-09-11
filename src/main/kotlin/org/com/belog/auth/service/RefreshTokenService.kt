package org.com.belog.auth.service

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.domain.RefreshToken
import org.com.belog.auth.repository.RefreshTokenRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.HexFormat

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {
    @Transactional
    fun saveOrUpdate(
        userId: Long,
        refreshToken: String,
        expiration: Duration,
    ) {
        val tokenHash = hash(refreshToken)
        val expiresAt = Instant.now(clock).plus(expiration)
        val savedToken = refreshTokenRepository.findByUserId(userId)

        if (savedToken != null) {
            savedToken.rotate(tokenHash, expiresAt)
            return
        }

        refreshTokenRepository.save(
            RefreshToken.issue(
                user = userRepository.getReferenceById(userId),
                tokenHash = tokenHash,
                expiresAt = expiresAt,
            ),
        )
    }

    @Transactional
    fun validateAndRotate(
        userId: Long,
        currentRefreshToken: String,
        newRefreshToken: String,
        expiration: Duration,
    ) {
        val savedToken =
            refreshTokenRepository.findByUserIdForUpdate(userId)
                ?: throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        val now = Instant.now(clock)

        if (!matches(currentRefreshToken, savedToken.tokenHash) || !savedToken.expiresAt.isAfter(now)) {
            throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        }

        savedToken.rotate(
            tokenHash = hash(newRefreshToken),
            expiresAt = now.plus(expiration),
        )
    }

    private fun matches(
        refreshToken: String,
        savedTokenHash: String,
    ): Boolean =
        MessageDigest.isEqual(
            hash(refreshToken).toByteArray(StandardCharsets.US_ASCII),
            savedTokenHash.toByteArray(StandardCharsets.US_ASCII),
        )

    private fun hash(refreshToken: String): String =
        HexFormat.of().formatHex(
            MessageDigest
                .getInstance(HASH_ALGORITHM)
                .digest(refreshToken.toByteArray(StandardCharsets.UTF_8)),
        )

    companion object {
        private const val HASH_ALGORITHM = "SHA-256"
    }
}
