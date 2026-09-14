package org.com.belog.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import java.time.Instant

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_users_provider_provider_user_id",
            columnNames = ["provider", "provider_user_id"],
        ),
        UniqueConstraint(
            name = "uk_users_nickname",
            columnNames = ["nickname"],
        ),
    ],
)
class User protected constructor(
    @Column(nullable = false, length = 320)
    val email: String,
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

    @Column(length = NICKNAME_MAX_LENGTH)
    var nickname: String? = null
        protected set

    @Column(name = "profile_image_object_key", length = PROFILE_IMAGE_OBJECT_KEY_MAX_LENGTH)
    var profileImageObjectKey: String? = null
        protected set

    @Embedded
    var bankAccount: BankAccount? = null
        protected set

    @Column(name = "onboarding_completed_at")
    var onboardingCompletedAt: Instant? = null
        protected set

    val isOnboardingCompleted: Boolean
        get() = onboardingCompletedAt != null

    fun completeOnboarding(
        profileImageObjectKey: String,
        nickname: String,
        bankAccount: BankAccount,
        completedAt: Instant,
    ) {
        check(!isOnboardingCompleted) { "이미 온보딩을 완료한 사용자입니다." }

        val normalizedNickname = nickname.trim()
        val normalizedProfileImageObjectKey = profileImageObjectKey.trim()

        require(normalizedNickname.length in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH) {
            "닉네임은 ${NICKNAME_MIN_LENGTH}자 이상 ${NICKNAME_MAX_LENGTH}자 이하여야 합니다."
        }
        require(normalizedProfileImageObjectKey.isNotEmpty()) { "프로필 이미지 object key는 비어 있을 수 없습니다." }
        require(normalizedProfileImageObjectKey.length <= PROFILE_IMAGE_OBJECT_KEY_MAX_LENGTH) {
            "프로필 이미지 object key는 ${PROFILE_IMAGE_OBJECT_KEY_MAX_LENGTH}자를 초과할 수 없습니다."
        }

        this.profileImageObjectKey = normalizedProfileImageObjectKey
        this.nickname = normalizedNickname
        this.bankAccount = bankAccount
        this.onboardingCompletedAt = completedAt
    }

    companion object {
        const val NICKNAME_MIN_LENGTH = 1
        const val NICKNAME_MAX_LENGTH = 8
        const val PROFILE_IMAGE_OBJECT_KEY_MAX_LENGTH = 1024

        fun createSocialUser(
            email: String,
            provider: SocialProvider,
            providerUserId: String,
        ): User {
            require(email.isNotBlank()) { "이메일은 비어 있을 수 없습니다." }
            require(providerUserId.isNotBlank()) { "소셜 사용자 식별자는 비어 있을 수 없습니다." }

            return User(
                email = email,
                provider = provider,
                providerUserId = providerUserId,
            )
        }
    }
}
