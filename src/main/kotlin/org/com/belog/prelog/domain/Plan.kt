package org.com.belog.prelog.domain

import jakarta.persistence.CheckConstraint
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    type: PlanType,
    category: PlanCategory,
    title: String,
    url: String?,
    content: String?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: PlanType = type
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var category: PlanCategory = category
        protected set

    @Column(nullable = false, length = PLAN_TITLE_MAX_LENGTH)
    var title: String = title
        protected set

    @Column(length = PLAN_URL_MAX_LENGTH)
    var url: String? = url
        protected set

    @Column(length = PLAN_CONTENT_MAX_LENGTH)
    var content: String? = content
        protected set

    @Column(nullable = false)
    var pinned: Boolean = false
        protected set

    @Embedded
    var location: PlanLocation? = null
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "location_status", nullable = false, length = 20)
    var locationStatus: LocationResolutionStatus = LocationResolutionStatus.NOT_APPLICABLE
        protected set

    fun pin() {
        pinned = true
    }

    fun unpin() {
        pinned = false
    }

    fun updateLink(
        category: PlanCategory,
        title: String,
        url: String,
    ) {
        val normalizedTitle = normalizeTitle(title)
        val normalizedUrl = normalizeUrl(url)

        if (this.url != normalizedUrl) {
            clearLocation()
        }

        this.type = PlanType.LINK
        this.category = category
        this.title = normalizedTitle
        this.url = normalizedUrl
        this.content = null
    }

    fun updateMemo(
        category: PlanCategory,
        title: String,
        content: String,
    ) {
        val normalizedTitle = normalizeTitle(title)
        val normalizedContent = normalizeContent(content)

        this.type = PlanType.MEMO
        this.category = category
        this.title = normalizedTitle
        this.url = null
        this.content = normalizedContent
        clearLocation()
    }

    fun resolveLocation(location: PlanLocation) {
        require(type == PlanType.LINK && url != null) {
            "링크 계획에만 위치 정보를 저장할 수 있습니다."
        }

        this.location = location
        this.locationStatus = LocationResolutionStatus.RESOLVED
    }

    fun failLocationResolution() {
        require(type == PlanType.LINK && url != null) {
            "링크 계획에만 위치 정보 추출 실패 상태를 기록할 수 있습니다."
        }

        this.location = null
        this.locationStatus = LocationResolutionStatus.FAILED
    }

    private fun clearLocation() {
        location = null
        locationStatus = LocationResolutionStatus.NOT_APPLICABLE
    }

    fun isCreatedBy(groupMember: GroupMember): Boolean {
        if (createdBy === groupMember) {
            return true
        }

        val creatorId = createdBy.id
        val groupMemberId = groupMember.id
        return creatorId != null && groupMemberId != null && creatorId == groupMemberId
    }

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
