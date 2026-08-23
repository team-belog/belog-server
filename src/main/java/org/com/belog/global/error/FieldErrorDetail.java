package org.com.belog.global.error;

public record FieldErrorDetail(
        String field,
        String reason
) {
}
