package org.com.belog.prelog.service

interface PlanLocationResolver {
    fun resolve(url: String): PlanLocationResolution
}
