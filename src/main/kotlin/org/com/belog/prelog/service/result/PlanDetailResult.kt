package org.com.belog.prelog.service.result

import org.com.belog.prelog.domain.PlanCategory

data class PlanDetailResult(
    val planId: Long,
    val category: PlanCategory,
    val title: String,
    val content: String,
    val canEdit: Boolean,
)
