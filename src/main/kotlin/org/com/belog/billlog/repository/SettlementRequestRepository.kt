package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.SettlementRequest
import org.springframework.data.jpa.repository.JpaRepository

interface SettlementRequestRepository : JpaRepository<SettlementRequest, Long>
