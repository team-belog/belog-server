package org.com.belog.prelog.service.result

import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.service.query.PlanListCursor
import java.time.Instant

data class PlanListResult(
    val items: List<PlanListItemResult>,
    val nextCursor: PlanListCursor?,
    val hasNext: Boolean,
)

data class PlanListItemResult(
    val planId: Long,
    val type: PlanType,
    val category: PlanCategory,
    val title: String,
    val url: String?,
    val content: String?,
    val thumbnailUrl: String?,
    val likeCount: Long,
    val likedByMe: Boolean,
    val pinned: Boolean,
    val canDelete: Boolean,
    val createdAt: Instant,
)
