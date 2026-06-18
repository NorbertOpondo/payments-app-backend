package com.app.payments.controller.dto;

import com.app.payments.model.PaymentMethod;
import com.app.payments.model.TransactionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String id;
    private BigDecimal amount;
    private String phoneNumber;
    private PaymentMethod paymentMethod;
    private TransactionStatus status;
    private LocalDateTime createdAt;
    private String receiptNumber;
    private String metadata;
}
