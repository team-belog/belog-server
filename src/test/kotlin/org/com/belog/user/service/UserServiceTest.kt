package org.com.belog.user.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DataJpaTest
@Import(UserService::class, JpaAuditingConfig::class)
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `처음 로그인한 소셜 사용자를 생성한다`() {
        val result = login()

        assertTrue(result.isNewUser)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `이미 가입한 소셜 사용자는 다시 생성하지 않는다`() {
        val firstLogin = login()
        val secondLogin = login()

        assertFalse(secondLogin.isNewUser)
        assertEquals(firstLogin.userId, secondLogin.userId)
        assertEquals(1, userRepository.count())
    }

    private fun login(): SocialUserResult =
        userService.findOrCreateSocialUser(
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-subject",
            email = "user@example.com",
            nickname = "belog",
            profileImageUrl = "https://example.com/profile.png",
        )
}
