package com.smbility.railcargo.common.exception;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final String code;
    private final String message;
    private final List<FieldErrorDetail> fieldErrors;

    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();

    @Getter
    @Builder
    public static class FieldErrorDetail {
        private final String field;
        private final String reason;
    }
}
