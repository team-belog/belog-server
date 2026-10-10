package org.com.belog.global.time

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

const val BUSINESS_TIME_ZONE = "Asia/Seoul"

private val BUSINESS_ZONE_ID: ZoneId = ZoneId.of(BUSINESS_TIME_ZONE)

fun Clock.currentBusinessDate(): LocalDate = LocalDate.ofInstant(instant(), BUSINESS_ZONE_ID)

fun Instant.toBusinessDate(): LocalDate = LocalDate.ofInstant(this, BUSINESS_ZONE_ID)

fun LocalDate.atStartOfBusinessDay(): Instant = atStartOfDay(BUSINESS_ZONE_ID).toInstant()
