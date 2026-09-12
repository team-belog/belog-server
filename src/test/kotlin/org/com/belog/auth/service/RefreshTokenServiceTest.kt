package org.com.belog.auth.service

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.domain.RefreshToken
import org.com.belog.auth.repository.RefreshTokenRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RefreshTokenServiceTest {
    private val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC)
    private val refreshTokenService = RefreshTokenService(refreshTokenRepository, userRepository, clock)

    @Test
    fun `저장된 토큰이 없으면 Refresh Token 해시와 만료 시각을 저장한다`() {
        val user = mock(User::class.java)
        `when`(userRepository.findByIdForUpdate(1L)).thenReturn(user)
        `when`(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(null)

        refreshTokenService.saveOrUpdate(1L, "refresh-token", Duration.ofDays(14))

        val captor = ArgumentCaptor.forClass(RefreshToken::class.java)
        verify(refreshTokenRepository).save(captor.capture())
        assertEquals(user, captor.value.user)
        assertEquals("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120", captor.value.tokenHash)
        assertEquals(Instant.parse("2026-09-25T00:00:00Z"), captor.value.expiresAt)
    }

    @Test
    fun `저장된 토큰이 있으면 새 Refresh Token 정보로 교체한다`() {
        val savedToken =
            RefreshToken.issue(
                user = mock(User::class.java),
                tokenHash = "old-token-hash",
                expiresAt = Instant.parse("2026-09-12T00:00:00Z"),
            )
        `when`(userRepository.findByIdForUpdate(1L)).thenReturn(savedToken.user)
        `when`(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(savedToken)

        refreshTokenService.saveOrUpdate(1L, "refresh-token", Duration.ofDays(14))

        assertEquals("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120", savedToken.tokenHash)
        assertEquals(Instant.parse("2026-09-25T00:00:00Z"), savedToken.expiresAt)
        verify(refreshTokenRepository, never()).save(savedToken)
    }

    @Test
    fun `저장된 Refresh Token이 일치하면 새로운 토큰으로 교체한다`() {
        val savedToken =
            RefreshToken.issue(
                user = mock(User::class.java),
                tokenHash = "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120",
                expiresAt = Instant.parse("2026-09-12T00:00:00Z"),
            )
        `when`(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(savedToken)

        refreshTokenService.validateAndRotate(
            userId = 1L,
            currentRefreshToken = "refresh-token",
            newRefreshToken = "new-refresh-token",
            expiration = Duration.ofDays(14),
        )

        assertEquals("c40dd1765d767caae2588f0ee1de9181d8a44cc9306261eb2c9e526351188338", savedToken.tokenHash)
        assertEquals(Instant.parse("2026-09-25T00:00:00Z"), savedToken.expiresAt)
    }

    @Test
    fun `저장된 Refresh Token과 일치하지 않으면 재발급할 수 없다`() {
        val savedToken =
            RefreshToken.issue(
                user = mock(User::class.java),
                tokenHash = "different-token-hash",
                expiresAt = Instant.parse("2026-09-12T00:00:00Z"),
            )
        `when`(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(savedToken)

        val exception =
            assertFailsWith<BusinessException> {
                refreshTokenService.validateAndRotate(
                    userId = 1L,
                    currentRefreshToken = "refresh-token",
                    newRefreshToken = "new-refresh-token",
                    expiration = Duration.ofDays(14),
                )
            }

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `저장된 Refresh Token이 만료되었으면 재발급할 수 없다`() {
        val savedToken =
            RefreshToken.issue(
                user = mock(User::class.java),
                tokenHash = "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120",
                expiresAt = Instant.parse("2026-09-11T00:00:00Z"),
            )
        `when`(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(savedToken)

        val exception =
            assertFailsWith<BusinessException> {
                refreshTokenService.validateAndRotate(
                    userId = 1L,
                    currentRefreshToken = "refresh-token",
                    newRefreshToken = "new-refresh-token",
                    expiration = Duration.ofDays(14),
                )
            }

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `저장된 Refresh Token이 없으면 재발급할 수 없다`() {
        `when`(refreshTokenRepository.findByUserIdForUpdate(1L)).thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                refreshTokenService.validateAndRotate(
                    userId = 1L,
                    currentRefreshToken = "refresh-token",
                    newRefreshToken = "new-refresh-token",
                    expiration = Duration.ofDays(14),
                )
            }

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }
}
