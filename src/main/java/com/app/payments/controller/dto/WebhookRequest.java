package com.app.payments.controller.dto;

import com.app.payments.model.TransactionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebhookRequest {
    private TransactionStatus status;
    private String resultCode;
    private String resultDesc;
    private String mpesaReceiptNumber;
}
