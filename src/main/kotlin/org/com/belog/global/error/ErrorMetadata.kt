package org.com.belog.global.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "오류 상세 정보")
@ConsistentCopyVisibility
data class ErrorMetadata private constructor(
    @field:Schema(description = "필드별 검증 오류 목록")
    val fieldErrors: List<FieldErrorDetail>,
    @field:Schema(description = "오류 발생 시각")
    val timestamp: Instant,
) {
    companion object {
        fun empty(): ErrorMetadata = of(emptyList())

        fun of(fieldErrors: List<FieldErrorDetail>): ErrorMetadata = ErrorMetadata(fieldErrors.toList(), Instant.now())
    }
}
