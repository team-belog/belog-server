package org.com.belog.global.time

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

private val BUSINESS_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

fun Clock.currentBusinessDate(): LocalDate = LocalDate.ofInstant(instant(), BUSINESS_ZONE_ID)
