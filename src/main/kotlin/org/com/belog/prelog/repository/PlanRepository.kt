package org.com.belog.prelog.repository

import org.com.belog.prelog.domain.Plan
import org.springframework.data.jpa.repository.JpaRepository

interface PlanRepository : JpaRepository<Plan, Long>
