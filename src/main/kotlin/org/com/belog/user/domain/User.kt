package org.com.belog.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_users_provider_provider_user_id",
            columnNames = ["provider", "provider_user_id"],
        ),
    ],
)
class User protected constructor(
    @Column(nullable = false, length = 320)
    val email: String,
    @Column(length = 100)
    val nickname: String?,
    @Column(name = "profile_image_url", length = 2048)
    val profileImageUrl: String?,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val provider: SocialProvider,
    @Column(name = "provider_user_id", nullable = false, length = 255)
    val providerUserId: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun createSocialUser(
            email: String,
            nickname: String?,
            profileImageUrl: String?,
            provider: SocialProvider,
            providerUserId: String,
        ): User {
            require(email.isNotBlank()) { "이메일은 비어 있을 수 없습니다." }
            require(providerUserId.isNotBlank()) { "소셜 사용자 식별자는 비어 있을 수 없습니다." }

            return User(
                email = email,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                provider = provider,
                providerUserId = providerUserId,
            )
        }
    }
}
