package com.app.payments.services.impl;

import com.app.payments.dto.PaymentRequest;
import com.app.payments.dto.PaymentResponse;
import com.app.payments.dto.WebhookRequest;
import com.app.payments.integrations.PaymentGatewayClient;
import com.app.payments.integrations.dto.GatewayRequest;
import com.app.payments.integrations.dto.GatewayResponse;
import com.app.payments.integrations.impl.MpesaCallbackSimulator;
import com.app.payments.model.PaymentMethod;
import com.app.payments.model.Transaction;
import com.app.payments.model.TransactionStatus;
import com.app.payments.repositories.PaymentRepository;
import com.app.payments.services.NotificationService;
import com.app.payments.services.PaymentService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;
    private final MpesaCallbackSimulator mpesaCallbackSimulator;
    private final Map<PaymentMethod, PaymentGatewayClient> gatewayClients;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            NotificationService notificationService,
            MpesaCallbackSimulator mpesaCallbackSimulator,
            @Qualifier("mpesaGatewayClient") PaymentGatewayClient mpesaGatewayClient,
            @Qualifier("cardGatewayClient") PaymentGatewayClient cardGatewayClient) {
        this.paymentRepository = paymentRepository;
        this.notificationService = notificationService;
        this.mpesaCallbackSimulator = mpesaCallbackSimulator;
        this.gatewayClients = Map.of(
                PaymentMethod.MPESA, mpesaGatewayClient,
                PaymentMethod.CARD, cardGatewayClient
        );
    }

    @Override
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        log.info("Initiating MPESA Payment : {}", request.getPhoneNumber()); //todo Concat
        Transaction transaction = paymentRepository.save(Transaction.builder()
                .amount(request.getAmount())
                .phoneNumber(request.getPhoneNumber())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.INITIATED)
                .build());

        transaction.setStatus(TransactionStatus.PROCESSING);
        paymentRepository.save(transaction);

        PaymentGatewayClient client = gatewayClients.get(request.getPaymentMethod());
        GatewayResponse gatewayResponse = client.processPayment(GatewayRequest.builder()
                .referenceId(transaction.getId())
                .amount(transaction.getAmount())
                .phoneNumber(transaction.getPhoneNumber())
                .build());

        log.info("new payment status : {}", gatewayResponse.getStatus());
        transaction.setStatus(gatewayResponse.getStatus());
        Transaction savedTransaction = paymentRepository.save(transaction);

        notificationService.notifyPaymentStatusChange(savedTransaction);

        if (savedTransaction.getStatus() == TransactionStatus.STK_PUSH_SENT) {
            mpesaCallbackSimulator.simulate(savedTransaction.getId());
        }

        return toResponse(savedTransaction);
    }

    @Override
    public PaymentResponse getPaymentStatus(String id) {
        return paymentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    @Override
    public PaymentResponse simulateWebhook(String id, WebhookRequest webhookRequest) {
        Transaction transaction = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        transaction.setStatus(webhookRequest.getStatus());
        Transaction saved = paymentRepository.save(transaction);

        notificationService.notifyPaymentStatusChange(saved);

        return toResponse(saved);
    }

    @Override
    public List<PaymentResponse> getTransactionHistory() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .phoneNumber(transaction.getPhoneNumber())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
