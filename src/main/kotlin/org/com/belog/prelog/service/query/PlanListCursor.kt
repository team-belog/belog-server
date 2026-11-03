package org.com.belog.prelog.service.query

data class PlanListCursor(
    val pinned: Boolean,
    val planId: Long,
) {
    init {
        require(planId > 0) { "계획 목록 커서의 계획 ID는 양수여야 합니다." }
    }
}
