package org.com.belog.billlog.repository

import org.com.belog.billlog.domain.Bill
import org.springframework.data.jpa.repository.JpaRepository

interface BillRepository : JpaRepository<Bill, Long>
