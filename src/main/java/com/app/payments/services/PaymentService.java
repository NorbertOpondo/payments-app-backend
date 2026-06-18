package com.app.payments.services;

import com.app.payments.dto.PaymentRequest;
import com.app.payments.dto.PaymentResponse;
import com.app.payments.dto.WebhookRequest;

import java.util.List;

public interface PaymentService {
    PaymentResponse initiatePayment(PaymentRequest request);
    PaymentResponse getPaymentStatus(String id);
    PaymentResponse simulateWebhook(String id, WebhookRequest webhookRequest);
    List<PaymentResponse> getTransactionHistory();
}
