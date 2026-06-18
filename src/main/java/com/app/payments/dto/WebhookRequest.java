package com.app.payments.dto;

import com.app.payments.model.TransactionStatus;
import lombok.Data;

@Data
public class WebhookRequest {
    private TransactionStatus status;
}
