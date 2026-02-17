package com.example.jwttemplate.global;

public record ApiResponse<T>(
    String message,
    T data
) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(message, null);
    }
}
