package org.com.belog.group.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity

const val GROUP_INVITE_CODE_UNIQUE_CONSTRAINT_NAME = "uk_groups_invite_code"
private const val GROUP_NAME_MIN_LENGTH = 1
private const val GROUP_NAME_MAX_LENGTH = 20

@Entity
@Table(
    name = "groups",
    uniqueConstraints = [
        UniqueConstraint(
            name = GROUP_INVITE_CODE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["invite_code"],
        ),
    ],
    check = [
        CheckConstraint(
            name = "chk_groups_name",
            constraint = "CHAR_LENGTH(TRIM(name)) BETWEEN $GROUP_NAME_MIN_LENGTH AND $GROUP_NAME_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_groups_invite_code_length",
            constraint = "CHAR_LENGTH(invite_code) = ${InviteCode.LENGTH}",
        ),
    ],
)
class Group protected constructor(
    @Column(nullable = false, length = GROUP_NAME_MAX_LENGTH)
    val name: String,
    @Column(name = "cover_image_object_key", length = GroupCoverImageObjectKey.MAX_LENGTH)
    val coverImageObjectKey: String?,
    @Column(name = "invite_code", nullable = false, length = InviteCode.LENGTH)
    val inviteCode: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        const val NAME_MIN_LENGTH = GROUP_NAME_MIN_LENGTH
        const val NAME_MAX_LENGTH = GROUP_NAME_MAX_LENGTH
        const val MAX_MEMBER_COUNT = 15

        fun create(
            name: String,
            coverImageObjectKey: GroupCoverImageObjectKey?,
            inviteCode: InviteCode,
        ): Group {
            val normalizedName = name.trim()
            require(normalizedName.length in NAME_MIN_LENGTH..NAME_MAX_LENGTH) {
                "그룹명은 ${NAME_MIN_LENGTH}자 이상 ${NAME_MAX_LENGTH}자 이하여야 합니다."
            }

            return Group(
                name = normalizedName,
                coverImageObjectKey = coverImageObjectKey?.value,
                inviteCode = inviteCode.value,
            )
        }
    }
}
