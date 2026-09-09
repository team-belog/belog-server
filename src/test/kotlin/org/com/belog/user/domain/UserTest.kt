package org.com.belog.user.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserTest {
    @Test
    fun `소셜 사용자 정보를 이용해 사용자를 생성한다`() {
        val user =
            User.createSocialUser(
                email = "user@example.com",
                nickname = "belog",
                profileImageUrl = "https://example.com/profile.png",
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
            )

        assertEquals("user@example.com", user.email)
        assertEquals("belog", user.nickname)
        assertEquals("https://example.com/profile.png", user.profileImageUrl)
        assertEquals(SocialProvider.GOOGLE, user.provider)
        assertEquals("google-subject", user.providerUserId)
    }

    @Test
    fun `이메일이 비어 있으면 사용자를 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            User.createSocialUser(
                email = " ",
                nickname = null,
                profileImageUrl = null,
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
            )
        }
    }

    @Test
    fun `소셜 사용자 식별자가 비어 있으면 사용자를 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            User.createSocialUser(
                email = "user@example.com",
                nickname = null,
                profileImageUrl = null,
                provider = SocialProvider.GOOGLE,
                providerUserId = " ",
            )
        }
    }
}
