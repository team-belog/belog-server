package org.com.belog.auth.service

import org.com.belog.auth.domain.AuthTokens
import org.com.belog.auth.domain.GoogleUserInfo
import org.com.belog.auth.infrastructure.JwtTokenProvider
import org.com.belog.auth.repository.RefreshTokenRepository
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LoginTransactionServiceConcurrencyTest {
    @Autowired
    private lateinit var loginTransactionService: LoginTransactionService

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var clock: Clock

    @AfterEach
    fun cleanUp() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `동일한 사용자가 동시에 로그인해도 Refresh Token은 한 개만 저장된다`() {
        `when`(clock.instant()).thenReturn(Instant.parse("2026-09-11T00:00:00Z"))
        val tokenSequence = AtomicInteger()
        `when`(jwtTokenProvider.createTokens(anyLong())).thenAnswer {
            val sequence = tokenSequence.incrementAndGet()
            AuthTokens(
                accessToken = "access-token-$sequence",
                refreshToken = "refresh-token-$sequence",
                accessTokenExpiration = Duration.ofMinutes(30),
                refreshTokenExpiration = Duration.ofDays(14),
            )
        }
        val googleUserInfo =
            GoogleUserInfo(
                providerUserId = "google-subject",
                email = "user@example.com",
            )
        val executor = Executors.newFixedThreadPool(CONCURRENT_LOGIN_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val logins =
                (1..CONCURRENT_LOGIN_COUNT).map {
                    executor.submit {
                        startSignal.await()
                        loginTransactionService.login(googleUserInfo)
                    }
                }

            startSignal.countDown()
            logins.forEach { login -> login.get(10, TimeUnit.SECONDS) }

            assertEquals(1, userRepository.count())
            assertEquals(1, refreshTokenRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `Refresh Token 저장에 실패하면 생성한 사용자도 롤백된다`() {
        `when`(jwtTokenProvider.createTokens(anyLong()))
            .thenReturn(
                AuthTokens(
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    accessTokenExpiration = Duration.ofMinutes(30),
                    refreshTokenExpiration = Duration.ofDays(14),
                ),
            )
        `when`(clock.instant()).thenThrow(IllegalStateException("Refresh Token 저장 실패"))
        val googleUserInfo =
            GoogleUserInfo(
                providerUserId = "google-subject",
                email = "user@example.com",
            )

        assertFailsWith<IllegalStateException> {
            loginTransactionService.login(googleUserInfo)
        }

        assertEquals(0, userRepository.count())
        assertEquals(0, refreshTokenRepository.count())
    }

    companion object {
        private const val CONCURRENT_LOGIN_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
