package com.app.payments.integrations.dto;

import com.app.payments.model.TransactionStatus;
import lombok.Builder;

import lombok.Data;

@Data
@Builder
public class GatewayResponse {
    private String referenceId;
    private TransactionStatus status;
    private String message;
}
