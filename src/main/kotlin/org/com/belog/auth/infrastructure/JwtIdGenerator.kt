package org.com.belog.auth.infrastructure

fun interface JwtIdGenerator {
    fun generate(): String
}
