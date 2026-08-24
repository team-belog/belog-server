package org.com.belog.global.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "필드 검증 오류")
public record FieldErrorDetail(
        @Schema(description = "오류가 발생한 필드", example = "title")
        String field,
        @Schema(description = "검증 실패 사유", example = "제목은 필수입니다.")
        String reason
) {
}
