package com.app.payments.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SmsResponse {
    private String id;
    private String phoneNumber;
    private String message;
    private String transactionId;
    private int status;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
