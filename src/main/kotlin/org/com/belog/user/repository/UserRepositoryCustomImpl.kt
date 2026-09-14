package org.com.belog.user.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

open class UserRepositoryCustomImpl(
    private val entityManager: EntityManager,
) : UserRepositoryCustom {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun upsertSocialUser(
        email: String,
        provider: SocialProvider,
        providerUserId: String,
    ): SocialUserUpsertResult {
        entityManager
            .createNativeQuery(UPSERT_SOCIAL_USER_SQL)
            .setParameter("email", email)
            .setParameter("provider", provider.name)
            .setParameter("providerUserId", providerUserId)
            .executeUpdate()

        val isNewUser = findLastInsertId() > 0
        val user = findSocialUserForUpdate(provider, providerUserId)

        return SocialUserUpsertResult(
            userId = requireNotNull(user.id),
            isNewUser = isNewUser,
        )
    }

    private fun findLastInsertId(): Long {
        val result = entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").singleResult
        return (result as Number).toLong()
    }

    private fun findSocialUserForUpdate(
        provider: SocialProvider,
        providerUserId: String,
    ): User =
        entityManager
            .createQuery(
                """
                SELECT user
                FROM User user
                WHERE user.provider = :provider
                  AND user.providerUserId = :providerUserId
                """.trimIndent(),
                User::class.java,
            ).setParameter("provider", provider)
            .setParameter("providerUserId", providerUserId)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .singleResult

    companion object {
        private const val UPSERT_SOCIAL_USER_SQL =
            """
            INSERT INTO users (
                email,
                provider,
                provider_user_id,
                created_at,
                updated_at
            ) VALUES (
                :email,
                :provider,
                :providerUserId,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE id = id + LAST_INSERT_ID(0)
            """
    }
}
