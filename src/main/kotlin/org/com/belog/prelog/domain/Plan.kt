package org.com.belog.prelog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.com.belog.global.domain.BaseEntity
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.meeting.domain.Meeting
import java.net.URI
import java.time.LocalDate

private const val PLAN_TITLE_MIN_LENGTH = 1
private const val PLAN_TITLE_MAX_LENGTH = 100
private const val PLAN_URL_MAX_LENGTH = 2048
private const val PLAN_CONTENT_MIN_LENGTH = 1
private const val PLAN_CONTENT_MAX_LENGTH = 2000

@Entity
@Table(
    name = "pre_log_plans",
    check = [
        CheckConstraint(
            name = "chk_pre_log_plans_title",
            constraint =
                "CHAR_LENGTH(TRIM(title)) BETWEEN $PLAN_TITLE_MIN_LENGTH AND $PLAN_TITLE_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_pre_log_plans_type_fields",
            constraint =
                "(type = 'LINK' AND url IS NOT NULL AND content IS NULL) OR " +
                    "(type = 'MEMO' AND url IS NULL AND content IS NOT NULL)",
        ),
        CheckConstraint(
            name = "chk_pre_log_plans_url",
            constraint = "url IS NULL OR CHAR_LENGTH(TRIM(url)) BETWEEN 1 AND $PLAN_URL_MAX_LENGTH",
        ),
        CheckConstraint(
            name = "chk_pre_log_plans_content",
            constraint =
                "content IS NULL OR " +
                    "CHAR_LENGTH(TRIM(content)) BETWEEN $PLAN_CONTENT_MIN_LENGTH AND $PLAN_CONTENT_MAX_LENGTH",
        ),
    ],
)
class Plan protected constructor(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "meeting_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_pre_log_plans_meeting_id"),
    )
    val meeting: Meeting,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "created_by_group_member_id",
        nullable = false,
        foreignKey = ForeignKey(name = "fk_pre_log_plans_created_by_group_member_id"),
    )
    val createdBy: GroupMember,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: PlanType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val category: PlanCategory,
    @Column(nullable = false, length = PLAN_TITLE_MAX_LENGTH)
    val title: String,
    @Column(length = PLAN_URL_MAX_LENGTH)
    val url: String?,
    @Column(length = PLAN_CONTENT_MAX_LENGTH)
    val content: String?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    companion object {
        const val TITLE_MIN_LENGTH = PLAN_TITLE_MIN_LENGTH
        const val TITLE_MAX_LENGTH = PLAN_TITLE_MAX_LENGTH
        const val URL_MAX_LENGTH = PLAN_URL_MAX_LENGTH
        const val CONTENT_MIN_LENGTH = PLAN_CONTENT_MIN_LENGTH
        const val CONTENT_MAX_LENGTH = PLAN_CONTENT_MAX_LENGTH

        fun createLink(
            meeting: Meeting,
            creator: GroupMember,
            category: PlanCategory,
            title: String,
            url: String,
            currentDate: LocalDate,
        ): Plan {
            validateCreation(meeting, creator, currentDate)

            return Plan(
                meeting = meeting,
                createdBy = creator,
                type = PlanType.LINK,
                category = category,
                title = normalizeTitle(title),
                url = normalizeUrl(url),
                content = null,
            )
        }

        fun createMemo(
            meeting: Meeting,
            creator: GroupMember,
            category: PlanCategory,
            title: String,
            content: String,
            currentDate: LocalDate,
        ): Plan {
            validateCreation(meeting, creator, currentDate)

            return Plan(
                meeting = meeting,
                createdBy = creator,
                type = PlanType.MEMO,
                category = category,
                title = normalizeTitle(title),
                url = null,
                content = normalizeContent(content),
            )
        }

        private fun validateCreation(
            meeting: Meeting,
            creator: GroupMember,
            currentDate: LocalDate,
        ) {
            require(creator.belongsTo(meeting.group)) {
                "계획 생성자는 해당 만남이 속한 그룹의 멤버여야 합니다."
            }
            require(!meeting.isEnded(currentDate)) {
                "종료된 만남에는 계획을 추가할 수 없습니다."
            }
        }

        private fun normalizeTitle(title: String): String {
            val normalizedTitle = title.trim()
            require(normalizedTitle.length in TITLE_MIN_LENGTH..TITLE_MAX_LENGTH) {
                "계획 제목은 ${TITLE_MIN_LENGTH}자 이상 ${TITLE_MAX_LENGTH}자 이하여야 합니다."
            }
            return normalizedTitle
        }

        private fun normalizeUrl(url: String): String {
            val normalizedUrl = url.trim()
            require(normalizedUrl.length in 1..URL_MAX_LENGTH) {
                "계획 URL은 비어 있을 수 없으며 ${URL_MAX_LENGTH}자를 초과할 수 없습니다."
            }

            val uri = runCatching { URI.create(normalizedUrl) }.getOrNull()
            require(
                uri != null &&
                    uri.isAbsolute &&
                    !uri.host.isNullOrBlank() &&
                    (
                        uri.scheme.equals("http", ignoreCase = true) ||
                            uri.scheme.equals("https", ignoreCase = true)
                    ),
            ) {
                "계획 URL은 유효한 HTTP 또는 HTTPS URL이어야 합니다."
            }
            return normalizedUrl
        }

        private fun normalizeContent(content: String): String {
            val normalizedContent = content.trim()
            require(normalizedContent.length in CONTENT_MIN_LENGTH..CONTENT_MAX_LENGTH) {
                "계획 내용은 ${CONTENT_MIN_LENGTH}자 이상 ${CONTENT_MAX_LENGTH}자 이하여야 합니다."
            }
            return normalizedContent
        }
    }
}

private fun GroupMember.belongsTo(group: Group): Boolean {
    if (this.group === group) {
        return true
    }

    val memberGroupId = this.group.id
    val groupId = group.id
    return memberGroupId != null && groupId != null && memberGroupId == groupId
}
