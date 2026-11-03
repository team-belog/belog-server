package org.com.belog.auth.config

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.global.error.ErrorMetadata
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.cors.DefaultCorsProcessor
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

class AuthCorsProcessor(
    private val objectMapper: ObjectMapper,
) : DefaultCorsProcessor() {
    override fun rejectRequest(response: ServerHttpResponse) {
        response.setStatusCode(AuthErrorCode.INVALID_REQUEST_ORIGIN.status)
        response.headers.contentType = MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8)
        objectMapper.writeValue(
            response.body,
            CommonResponse.error(AuthErrorCode.INVALID_REQUEST_ORIGIN, ErrorMetadata.empty()),
        )
        response.flush()
    }
}
