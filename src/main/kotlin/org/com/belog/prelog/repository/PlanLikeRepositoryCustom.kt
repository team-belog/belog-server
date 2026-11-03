package org.com.belog.prelog.repository

interface PlanLikeRepositoryCustom {
    fun saveIfAbsent(
        planId: Long,
        groupMemberId: Long,
    )
}
