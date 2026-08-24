package org.com.belog.global.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(description = "오류 상세 정보")
public record ErrorMetadata(
        @Schema(description = "필드별 검증 오류 목록")
        List<FieldErrorDetail> fieldErrors,
        @Schema(description = "오류 발생 시각")
        Instant timestamp
) {

    public ErrorMetadata {
        fieldErrors = List.copyOf(fieldErrors);
    }

    public static ErrorMetadata empty() {
        return of(List.of());
    }

    public static ErrorMetadata of(List<FieldErrorDetail> fieldErrors) {
        Objects.requireNonNull(fieldErrors, "fieldErrors must not be null");
        return new ErrorMetadata(fieldErrors, Instant.now());
    }
}
