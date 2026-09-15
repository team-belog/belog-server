package org.com.belog.user.domain

import jakarta.persistence.CheckConstraint
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
import java.net.URI
import java.time.Instant

const val USER_NICKNAME_UNIQUE_CONSTRAINT_NAME = "uk_users_nickname"

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_users_provider_provider_user_id",
            columnNames = ["provider", "provider_user_id"],
        ),
        UniqueConstraint(
            name = USER_NICKNAME_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["nickname"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_users_onboarding_state",
            constraint =
                "(" +
                    "onboarding_completed_at IS NULL AND nickname IS NULL AND profile_image_object_key IS NULL " +
                    "AND bank IS NULL AND encrypted_account_number IS NULL AND account_holder_name IS NULL" +
                    ") OR (" +
                    "onboarding_completed_at IS NOT NULL AND nickname IS NOT NULL " +
                    "AND bank IS NOT NULL AND encrypted_account_number IS NOT NULL AND account_holder_name IS NOT NULL" +
                    ")",
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

    @Column(name = "profile_image_object_key", length = ProfileImageObjectKey.MAX_LENGTH)
    var profileImageObjectKey: String? = null
        protected set

    @Column(name = "social_profile_image_url", length = SOCIAL_PROFILE_IMAGE_URL_MAX_LENGTH)
    var socialProfileImageUrl: String? = null
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
        profileImageObjectKey: ProfileImageObjectKey?,
        nickname: String,
        bankAccount: BankAccount,
        completedAt: Instant,
    ) {
        check(!isOnboardingCompleted) { "이미 온보딩을 완료한 사용자입니다." }

        val normalizedNickname = nickname.trim()

        require(normalizedNickname.length in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH) {
            "닉네임은 ${NICKNAME_MIN_LENGTH}자 이상 ${NICKNAME_MAX_LENGTH}자 이하여야 합니다."
        }

        this.profileImageObjectKey = profileImageObjectKey?.value
        this.nickname = normalizedNickname
        this.bankAccount = bankAccount
        this.onboardingCompletedAt = completedAt
    }

    fun updateSocialProfileImageUrl(socialProfileImageUrl: String?) {
        if (socialProfileImageUrl != null) {
            this.socialProfileImageUrl = normalizeSocialProfileImageUrl(socialProfileImageUrl)
        }
    }

    companion object {
        const val NICKNAME_MIN_LENGTH = 1
        const val NICKNAME_MAX_LENGTH = 8
        const val SOCIAL_PROFILE_IMAGE_URL_MAX_LENGTH = 2048

        fun createSocialUser(
            email: String,
            provider: SocialProvider,
            providerUserId: String,
            socialProfileImageUrl: String? = null,
        ): User {
            require(email.isNotBlank()) { "이메일은 비어 있을 수 없습니다." }
            require(providerUserId.isNotBlank()) { "소셜 사용자 식별자는 비어 있을 수 없습니다." }

            return User(
                email = email,
                provider = provider,
                providerUserId = providerUserId,
            ).apply { updateSocialProfileImageUrl(socialProfileImageUrl) }
        }

        private fun normalizeSocialProfileImageUrl(value: String): String {
            val normalizedValue = value.trim()
            require(normalizedValue.length <= SOCIAL_PROFILE_IMAGE_URL_MAX_LENGTH) {
                "소셜 프로필 이미지 URL은 ${SOCIAL_PROFILE_IMAGE_URL_MAX_LENGTH}자를 초과할 수 없습니다."
            }
            val uri = URI.create(normalizedValue)
            require(uri.scheme == "https" && !uri.host.isNullOrBlank()) {
                "소셜 프로필 이미지 URL은 유효한 HTTPS URL이어야 합니다."
            }
            return normalizedValue
        }
    }
}
