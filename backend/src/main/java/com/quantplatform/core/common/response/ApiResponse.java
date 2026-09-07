package com.quantplatform.core.common.response;

import java.time.Instant;
import java.util.List;

/**
 * Standardized envelope for every API response in the platform, success or
 * failure, so frontend consumers never branch on response shape.
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        Instant timestamp,
        List<String> errors
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, Instant.now(), null);
    }

    public static <T> ApiResponse<T> error(String message, List<String> errors) {
        return new ApiResponse<>(false, null, message, Instant.now(), errors);
    }
}
