package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.BillItem
import org.springframework.data.jpa.repository.JpaRepository

interface BillItemRepository : JpaRepository<BillItem, Long>
