package com.app.payments.controller;

import com.app.payments.controller.dto.PaymentRequest;
import com.app.payments.controller.dto.PaymentResponse;
import com.app.payments.controller.dto.WebhookRequest;
import com.app.payments.exceptions.PaymentException;
import com.app.payments.model.PaymentMethod;
import com.app.payments.model.TransactionStatus;
import com.app.payments.security.JwtService;
import com.app.payments.services.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean PaymentService paymentService;
    @MockitoBean JwtService jwtService;

    private PaymentResponse paymentResponse(TransactionStatus status) {
        return PaymentResponse.builder()
                .id("tx-123")
                .amount(new BigDecimal("500.00"))
                .phoneNumber("+254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // POST /api/v1/payments

    @Test
    @WithMockUser
    void initiatePayment_validRequest_returns201WithData() throws Exception {
        given(paymentService.initiatePayment(any(), any()))
                .willReturn(paymentResponse(TransactionStatus.STK_PUSH_SENT));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPhoneNumber("+254712345678");
        request.setPaymentMethod(PaymentMethod.MPESA);

        mockMvc.perform(post("/api/v1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("tx-123"))
                .andExpect(jsonPath("$.data.status").value("STK_PUSH_SENT"));
    }

    @Test
    @WithMockUser
    void initiatePayment_forwardsIdempotencyKeyToService() throws Exception {
        given(paymentService.initiatePayment(any(), eq("idem-key-1")))
                .willReturn(paymentResponse(TransactionStatus.STK_PUSH_SENT));

        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPhoneNumber("+254712345678");
        request.setPaymentMethod(PaymentMethod.MPESA);

        mockMvc.perform(post("/api/v1/payments")
                        .with(csrf())
                        .header("Idempotency-Key", "idem-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    void initiatePayment_missingAmount_returns400WithErrors() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setPhoneNumber("+254712345678");
        request.setPaymentMethod(PaymentMethod.MPESA);
        // amount omitted so that it will violate NotNull check

        mockMvc.perform(post("/api/v1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    @Test
    void initiatePayment_unauthenticated_deniesAccess() throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setPhoneNumber("+254712345678");
        request.setPaymentMethod(PaymentMethod.MPESA);

        mockMvc.perform(post("/api/v1/payments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    // GET /api/v1/payments/{id}

    @Test
    @WithMockUser
    void getPaymentStatus_existingId_returns200WithData() throws Exception {
        given(paymentService.getPaymentStatus("tx-123"))
                .willReturn(paymentResponse(TransactionStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/payments/tx-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("tx-123"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void getPaymentStatus_notFound_returns404WithErrors() throws Exception {
        given(paymentService.getPaymentStatus("bad-id"))
                .willThrow(PaymentException.notFound("Transaction not found: bad-id"));

        mockMvc.perform(get("/api/v1/payments/bad-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors").value("Transaction not found: bad-id"));
    }

    // GET /api/v1/payments

    @Test
    @WithMockUser
    void getTransactionHistory_returns200WithList() throws Exception {
        given(paymentService.getTransactionHistory()).willReturn(List.of(
                paymentResponse(TransactionStatus.COMPLETED),
                paymentResponse(TransactionStatus.FAILED)
        ));

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @WithMockUser
    void getTransactionHistory_empty_returnsEmptyArray() throws Exception {
        given(paymentService.getTransactionHistory()).willReturn(List.of());

        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // POST /api/v1/payments/{id}/webhook

    @Test
    @WithMockUser
    void simulateWebhook_validRequest_returns200WithUpdatedStatus() throws Exception {
        given(paymentService.simulateWebhook(eq("tx-123"), any()))
                .willReturn(paymentResponse(TransactionStatus.COMPLETED));

        WebhookRequest webhookRequest = new WebhookRequest();
        webhookRequest.setStatus(TransactionStatus.COMPLETED);

        mockMvc.perform(post("/api/v1/payments/tx-123/webhook")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void simulateWebhook_notFound_returns404() throws Exception {
        given(paymentService.simulateWebhook(eq("bad-id"), any()))
                .willThrow(PaymentException.notFound("Transaction not found: bad-id"));

        WebhookRequest webhookRequest = new WebhookRequest();
        webhookRequest.setStatus(TransactionStatus.FAILED);

        mockMvc.perform(post("/api/v1/payments/bad-id/webhook")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isNotFound());
    }
}
