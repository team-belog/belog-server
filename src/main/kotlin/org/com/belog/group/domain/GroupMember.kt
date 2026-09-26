package org.com.belog.group.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.com.belog.global.domain.BaseEntity
import org.com.belog.user.domain.User

const val GROUP_MEMBER_UNIQUE_CONSTRAINT_NAME = "uk_group_members_group_user"

@Entity
@Table(
    name = "group_members",
    uniqueConstraints = [
        UniqueConstraint(
            name = GROUP_MEMBER_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["group_id", "user_id"],
        ),
    ],
    indexes = [
        Index(name = "idx_group_members_user_id", columnList = "user_id"),
    ],
)
class GroupMember protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "group_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_group_members_group_id"),
    )
    val group: Group,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_group_members_user_id"),
    )
    val user: User,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: GroupRole,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    internal fun belongsTo(group: Group): Boolean {
        if (this.group === group) {
            return true
        }

        val memberGroupId = this.group.id
        val groupId = group.id
        return memberGroupId != null && groupId != null && memberGroupId == groupId
    }

    companion object {
        fun createOwner(
            group: Group,
            user: User,
        ): GroupMember {
            require(user.isOnboardingCompleted) { "온보딩을 완료한 사용자만 그룹을 생성할 수 있습니다." }

            return GroupMember(
                group = group,
                user = user,
                role = GroupRole.OWNER,
            )
        }

        fun createMember(
            group: Group,
            user: User,
        ): GroupMember {
            require(user.isOnboardingCompleted) { "온보딩을 완료한 사용자만 그룹에 참여할 수 있습니다." }

            return GroupMember(
                group = group,
                user = user,
                role = GroupRole.MEMBER,
            )
        }
    }
}
