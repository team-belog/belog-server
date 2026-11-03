package org.com.belog.prelog.domain

import jakarta.persistence.Entity
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
import org.com.belog.group.domain.GroupMember

const val PLAN_LIKE_UNIQUE_CONSTRAINT_NAME = "uk_pre_log_plan_likes_plan_member"

@Entity
@Table(
    name = "pre_log_plan_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = PLAN_LIKE_UNIQUE_CONSTRAINT_NAME,
            columnNames = ["plan_id", "group_member_id"],
        ),
    ],
    indexes = [
        Index(
            name = "idx_pre_log_plan_likes_group_member_id_plan_id",
            columnList = "group_member_id, plan_id",
        ),
    ],
)
class PlanLike protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "plan_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_pre_log_plan_likes_plan_id"),
    )
    val plan: Plan,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_pre_log_plan_likes_group_member_id"),
    )
    val groupMember: GroupMember,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        fun create(
            plan: Plan,
            groupMember: GroupMember,
        ): PlanLike {
            require(groupMember.belongsTo(plan.meeting.group)) {
                "좋아요를 등록하는 사용자는 계획이 속한 그룹의 멤버여야 합니다."
            }

            return PlanLike(
                plan = plan,
                groupMember = groupMember,
            )
        }
    }
}
