package org.com.belog.prelog.service.result

data class PlanLikeResult(
    val planId: Long,
    val likedByMe: Boolean,
    val likeCount: Long,
)
