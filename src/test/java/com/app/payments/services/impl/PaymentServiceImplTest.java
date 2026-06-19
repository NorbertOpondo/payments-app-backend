package com.app.payments.services.impl;

import com.app.payments.controller.dto.PaymentRequest;
import com.app.payments.controller.dto.PaymentResponse;
import com.app.payments.controller.dto.WebhookRequest;
import com.app.payments.exceptions.PaymentException;
import com.app.payments.integrations.PaymentGatewayClient;
import com.app.payments.integrations.dto.GatewayResponse;
import com.app.payments.integrations.impl.AsyncGatewayProcessor;
import com.app.payments.integrations.dto.GatewayRequest;
import com.app.payments.model.PaymentMethod;
import com.app.payments.model.Transaction;
import com.app.payments.model.TransactionStatus;
import com.app.payments.repositories.PaymentRepository;
import com.app.payments.services.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AsyncGatewayProcessor asyncGatewayProcessor;
    @Mock private PaymentGatewayClient cardGatewayClient;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                notificationService,
                asyncGatewayProcessor,
                cardGatewayClient
        );
    }

    // Simulates JPA UUID generation: first save gets an ID, subsequent saves pass through
    private void mockSaveWithIdGeneration() {
        given(paymentRepository.save(any(Transaction.class))).willAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            if (tx.getId() == null) {
                tx.setId("tx-123");
            }
            return tx;
        });
    }

    private Transaction buildTransaction(TransactionStatus status) {
        return Transaction.builder()
                .id("tx-123")
                .amount(new BigDecimal("500.00"))
                .phoneNumber("+254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private PaymentRequest mpesaRequest() {
        PaymentRequest r = new PaymentRequest();
        r.setAmount(new BigDecimal("500.00"));
        r.setPhoneNumber("+254712345678");
        r.setPaymentMethod(PaymentMethod.MPESA);
        return r;
    }

    private PaymentRequest cardRequest() {
        PaymentRequest r = new PaymentRequest();
        r.setAmount(new BigDecimal("500.00"));
        r.setPhoneNumber("+254000004242");
        r.setPaymentMethod(PaymentMethod.CARD);
        return r;
    }

    // ── initiatePayment ────────────────────────────────────────────────────────

    @Test
    void initiatePayment_mpesa_dispatchesAsyncAndReturnsProcessing() {
        mockSaveWithIdGeneration();

        PaymentResponse response = paymentService.initiatePayment(mpesaRequest(), null);

        // M-Pesa is fire-and-forget: caller gets PROCESSING immediately
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.PROCESSING);
        verify(asyncGatewayProcessor).processMpesaAsync(eq("tx-123"), any());
        // Only INITIATED + PROCESSING saves happen synchronously
        verify(paymentRepository, times(2)).save(any());
        // Notification is sent inside AsyncGatewayProcessor, not here
        verifyNoInteractions(notificationService);
        verifyNoInteractions(cardGatewayClient);
    }

    @Test
    void initiatePayment_card_completed_setsReceiptNumberWithCrdPrefix() {
        mockSaveWithIdGeneration();
        given(cardGatewayClient.processPayment(any()))
                .willReturn(GatewayResponse.builder().status(TransactionStatus.COMPLETED).build());

        PaymentResponse response = paymentService.initiatePayment(cardRequest(), null);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getReceiptNumber()).startsWith("CRD");
        verifyNoInteractions(mpesaCallbackSimulator);
    }

    @Test
    void initiatePayment_card_failed_noReceiptNumber() {
        mockSaveWithIdGeneration();
        given(cardGatewayClient.processPayment(any()))
                .willReturn(GatewayResponse.builder().status(TransactionStatus.FAILED).build());

        PaymentResponse response = paymentService.initiatePayment(cardRequest(), null);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getReceiptNumber()).isNull();
    }

    @Test
    void initiatePayment_existingIdempotencyKey_returnsExistingWithoutGatewayCall() {
        given(paymentRepository.findByIdempotencyKey("key-abc"))
                .willReturn(Optional.of(buildTransaction(TransactionStatus.STK_PUSH_SENT)));

        PaymentResponse response = paymentService.initiatePayment(mpesaRequest(), "key-abc");

        assertThat(response.getId()).isEqualTo("tx-123");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.STK_PUSH_SENT);
        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(mpesaGatewayClient, cardGatewayClient);
    }

    @Test
    void initiatePayment_nullIdempotencyKey_skipsIdempotencyCheck() {
        mockSaveWithIdGeneration();

        paymentService.initiatePayment(mpesaRequest(), null);

        verify(paymentRepository, never()).findByIdempotencyKey(any());
    }

    @Test
    void initiatePayment_storesIdempotencyKeyOnNewTransaction() {
        mockSaveWithIdGeneration();

        paymentService.initiatePayment(mpesaRequest(), "unique-key-xyz");

        verify(paymentRepository, atLeastOnce()).save(argThat(tx ->
                "unique-key-xyz".equals(tx.getIdempotencyKey())
        ));
    }

    // ── getPaymentStatus ───────────────────────────────────────────────────────

    @Test
    void getPaymentStatus_existingId_mapsAllFields() {
        given(paymentRepository.findById("tx-123"))
                .willReturn(Optional.of(buildTransaction(TransactionStatus.COMPLETED)));

        PaymentResponse response = paymentService.getPaymentStatus("tx-123");

        assertThat(response.getId()).isEqualTo("tx-123");
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
        assertThat(response.getPhoneNumber()).isEqualTo("+254712345678");
        assertThat(response.getPaymentMethod()).isEqualTo(PaymentMethod.MPESA);
    }

    @Test
    void getPaymentStatus_notFound_throwsPaymentExceptionWith404() {
        given(paymentRepository.findById("missing")).willReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentStatus("missing"))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("missing")
                .satisfies(ex -> assertThat(((PaymentException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── simulateWebhook ────────────────────────────────────────────────────────

    @Test
    void simulateWebhook_updatesStatusAndNotifies() {
        Transaction tx = buildTransaction(TransactionStatus.STK_PUSH_SENT);
        given(paymentRepository.findById("tx-123")).willReturn(Optional.of(tx));
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WebhookRequest req = new WebhookRequest();
        req.setStatus(TransactionStatus.COMPLETED);

        PaymentResponse response = paymentService.simulateWebhook("tx-123", req);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(notificationService).notifyPaymentStatusChange(any());
    }

    @Test
    void simulateWebhook_withReceiptNumber_setsItOnTransaction() {
        Transaction tx = buildTransaction(TransactionStatus.STK_PUSH_SENT);
        given(paymentRepository.findById("tx-123")).willReturn(Optional.of(tx));
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WebhookRequest req = new WebhookRequest();
        req.setStatus(TransactionStatus.COMPLETED);
        req.setMpesaReceiptNumber("LGR9X2K8Z");

        paymentService.simulateWebhook("tx-123", req);

        assertThat(tx.getReceiptNumber()).isEqualTo("LGR9X2K8Z");
    }

    @Test
    void simulateWebhook_withoutReceiptNumber_doesNotOverwrite() {
        Transaction tx = buildTransaction(TransactionStatus.STK_PUSH_SENT);
        tx.setReceiptNumber("EXISTING");
        given(paymentRepository.findById("tx-123")).willReturn(Optional.of(tx));
        given(paymentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        WebhookRequest req = new WebhookRequest();
        req.setStatus(TransactionStatus.COMPLETED);
        // no receipt number set

        paymentService.simulateWebhook("tx-123", req);

        assertThat(tx.getReceiptNumber()).isEqualTo("EXISTING");
    }

    @Test
    void simulateWebhook_notFound_throwsPaymentExceptionWith404() {
        given(paymentRepository.findById("bad-id")).willReturn(Optional.empty());

        WebhookRequest req = new WebhookRequest();
        req.setStatus(TransactionStatus.COMPLETED);

        assertThatThrownBy(() -> paymentService.simulateWebhook("bad-id", req))
                .isInstanceOf(PaymentException.class)
                .satisfies(ex -> assertThat(((PaymentException) ex).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ── getTransactionHistory ──────────────────────────────────────────────────

    @Test
    void getTransactionHistory_returnsAllMappedResponses() {
        given(paymentRepository.findAll()).willReturn(List.of(
                buildTransaction(TransactionStatus.COMPLETED),
                buildTransaction(TransactionStatus.FAILED)
        ));

        List<PaymentResponse> history = paymentService.getTransactionHistory();

        assertThat(history).hasSize(2);
        assertThat(history).extracting(PaymentResponse::getStatus)
                .containsExactlyInAnyOrder(TransactionStatus.COMPLETED, TransactionStatus.FAILED);
    }

    @Test
    void getTransactionHistory_empty_returnsEmptyList() {
        given(paymentRepository.findAll()).willReturn(List.of());

        assertThat(paymentService.getTransactionHistory()).isEmpty();
    }
}
