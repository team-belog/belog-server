package org.com.belog.prelog.service.result

import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import java.time.Instant

data class PlanListResult(
    val items: List<PlanListItemResult>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class PlanListItemResult(
    val planId: Long,
    val type: PlanType,
    val category: PlanCategory,
    val title: String,
    val url: String?,
    val address: String?,
    val thumbnailUrl: String?,
    val likeCount: Long,
    val likedByMe: Boolean,
    val pinned: Boolean,
    val canDelete: Boolean,
    val createdAt: Instant,
)
