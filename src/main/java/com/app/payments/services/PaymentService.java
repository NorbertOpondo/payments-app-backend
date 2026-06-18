package com.app.payments.services;

import com.app.payments.controller.dto.PaymentRequest;
import com.app.payments.controller.dto.PaymentResponse;
import com.app.payments.controller.dto.WebhookRequest;

import java.util.List;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentRequest request, String idempotencyKey);
    PaymentResponse getPaymentStatus(String id);
    PaymentResponse simulateWebhook(String id, WebhookRequest webhookRequest);
    List<PaymentResponse> getTransactionHistory();
}
