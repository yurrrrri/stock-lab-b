package com.stocklab.core.api.exception;

import java.util.Map;

public record ApiErrorResponse(
        ApiErrorCode code,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(ApiErrorCode code, String message) {
        return new ApiErrorResponse(code, message, Map.of());
    }
}
