package org.com.belog.global.error;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ErrorMetadata(
        List<FieldErrorDetail> fieldErrors,
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
