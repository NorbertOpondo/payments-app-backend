package com.app.payments.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String description;
    private T data;
    private String errors;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .description("Success")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(200)
                .description("Created successfully")
                .data(data)
                .build();
    }

    public static ApiResponse<Void> error(String errors) {
        return ApiResponse.<Void>builder()
                .status(400)
                .description("Request failed")
                .errors(errors)
                .build();
    }
}
