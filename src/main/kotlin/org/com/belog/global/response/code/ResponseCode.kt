package org.com.belog.global.response.code

import org.springframework.http.HttpStatus

interface ResponseCode {
    val status: HttpStatus
    val code: String
    val message: String
}
