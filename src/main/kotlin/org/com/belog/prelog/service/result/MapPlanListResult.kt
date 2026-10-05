package org.com.belog.prelog.service.result

import org.com.belog.prelog.domain.PlanCategory
import java.math.BigDecimal
import java.time.Instant

data class MapPlanListResult(
    val items: List<MapPlanListItemResult>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class MapPlanListItemResult(
    val planId: Long,
    val category: PlanCategory,
    val title: String,
    val url: String,
    val address: String?,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val pinned: Boolean,
    val createdAt: Instant,
)
