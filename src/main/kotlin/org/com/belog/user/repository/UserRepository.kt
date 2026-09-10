package org.com.belog.user.repository

import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByProviderAndProviderUserId(
        provider: SocialProvider,
        providerUserId: String,
    ): User?
}
