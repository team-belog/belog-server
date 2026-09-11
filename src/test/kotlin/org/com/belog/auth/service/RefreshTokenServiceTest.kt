package org.com.belog.auth.service

import org.com.belog.auth.domain.RefreshToken
import org.com.belog.auth.repository.RefreshTokenRepository
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

class RefreshTokenServiceTest {
    private val refreshTokenRepository = mock(RefreshTokenRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-11T00:00:00Z"), ZoneOffset.UTC)
    private val refreshTokenService = RefreshTokenService(refreshTokenRepository, userRepository, clock)

    @Test
    fun `저장된 토큰이 없으면 Refresh Token 해시와 만료 시각을 저장한다`() {
        val user = mock(User::class.java)
        `when`(refreshTokenRepository.findByUserId(1L)).thenReturn(null)
        `when`(userRepository.getReferenceById(1L)).thenReturn(user)

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
        `when`(refreshTokenRepository.findByUserId(1L)).thenReturn(savedToken)

        refreshTokenService.saveOrUpdate(1L, "refresh-token", Duration.ofDays(14))

        assertEquals("0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120", savedToken.tokenHash)
        assertEquals(Instant.parse("2026-09-25T00:00:00Z"), savedToken.expiresAt)
        verify(userRepository, never()).getReferenceById(1L)
        verify(refreshTokenRepository, never()).save(savedToken)
    }
}
