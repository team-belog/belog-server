package org.com.belog.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.com.belog.global.domain.BaseEntity
import org.com.belog.user.domain.User
import java.time.Instant

@Entity
@Table(name = "refresh_tokens")
class RefreshToken protected constructor(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        unique = true,
        foreignKey = ForeignKey(name = "fk_refresh_tokens_user_id"),
    )
    val user: User,
    tokenHash: String,
    expiresAt: Instant,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(name = "token_hash", nullable = false, length = 64)
    var tokenHash: String = tokenHash
        protected set

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant = expiresAt
        protected set

    fun rotate(
        tokenHash: String,
        expiresAt: Instant,
    ) {
        require(tokenHash.isNotBlank()) { "Refresh Token 해시는 비어 있을 수 없습니다." }

        this.tokenHash = tokenHash
        this.expiresAt = expiresAt
    }

    companion object {
        fun issue(
            user: User,
            tokenHash: String,
            expiresAt: Instant,
        ): RefreshToken {
            require(tokenHash.isNotBlank()) { "Refresh Token 해시는 비어 있을 수 없습니다." }

            return RefreshToken(
                user = user,
                tokenHash = tokenHash,
                expiresAt = expiresAt,
            )
        }
    }
}
