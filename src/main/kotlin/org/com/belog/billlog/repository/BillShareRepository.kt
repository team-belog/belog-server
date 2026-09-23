package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.BillShare
import org.springframework.data.jpa.repository.JpaRepository

interface BillShareRepository : JpaRepository<BillShare, Long>
