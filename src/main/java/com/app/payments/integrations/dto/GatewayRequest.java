package com.app.payments.integrations.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GatewayRequest {
    private String referenceId;
    private BigDecimal amount;
    private String phoneNumber;
}
