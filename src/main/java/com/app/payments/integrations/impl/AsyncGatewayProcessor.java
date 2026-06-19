package com.app.payments.integrations.impl;

import com.app.payments.integrations.PaymentGatewayClient;
import com.app.payments.integrations.dto.GatewayRequest;
import com.app.payments.integrations.dto.GatewayResponse;
import com.app.payments.model.Transaction;
import com.app.payments.model.TransactionStatus;
import com.app.payments.repositories.PaymentRepository;
import com.app.payments.services.NotificationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AsyncGatewayProcessor {

    private final PaymentGatewayClient mpesaGatewayClient;
    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final MpesaCallbackSimulator mpesaCallbackSimulator;

    public AsyncGatewayProcessor(
            @Qualifier("mpesaGatewayClient") PaymentGatewayClient mpesaGatewayClient,
            PaymentRepository paymentRepository,
            NotificationService notificationService,
            MpesaCallbackSimulator mpesaCallbackSimulator) {
        this.mpesaGatewayClient = mpesaGatewayClient;
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.mpesaCallbackSimulator = mpesaCallbackSimulator;
    }

    @Async
    @Transactional
    public void processMpesaAsync(String transactionId, GatewayRequest gatewayRequest) {
        GatewayResponse response = mpesaGatewayClient.processPayment(gatewayRequest);

        Transaction transaction = paymentRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found after async dispatch: " + transactionId));

        transaction.setStatus(response.getStatus());
        Transaction saved = paymentRepository.save(transaction);
        log.info("M-Pesa async result for {}: {}", transactionId, response.getStatus());

        notificationService.notifyPaymentStatusChange(saved);

        if (saved.getStatus() == TransactionStatus.STK_PUSH_SENT) {
            mpesaCallbackSimulator.simulate(saved.getId());
        }
    }
}
