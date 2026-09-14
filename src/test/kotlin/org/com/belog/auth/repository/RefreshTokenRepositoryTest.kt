package org.com.belog.auth.repository

import org.com.belog.auth.domain.RefreshToken
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class RefreshTokenRepositoryTest {
    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `사용자 ID로 저장된 Refresh Token을 조회한다`() {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "user@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "google-subject",
                ),
            )
        val savedToken =
            refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(
                    user = user,
                    tokenHash = "0eb17643d4e9261163783a420859c92c7d212fa9624106a12b510afbec266120",
                    expiresAt = Instant.parse("2026-09-25T00:00:00Z"),
                ),
            )

        val foundToken = refreshTokenRepository.findByUserId(requireNotNull(user.id))

        assertNotNull(foundToken)
        assertEquals(savedToken.id, foundToken.id)
        assertEquals(savedToken.tokenHash, foundToken.tokenHash)
        assertEquals(savedToken.expiresAt, foundToken.expiresAt)
    }
}
