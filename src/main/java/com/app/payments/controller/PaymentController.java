package com.app.payments.controller;

import com.app.payments.controller.dto.ApiResponse;
import com.app.payments.controller.dto.PaymentRequest;
import com.app.payments.controller.dto.PaymentResponse;
import com.app.payments.controller.dto.WebhookRequest;
import com.app.payments.services.PaymentService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        if (!rateLimiter.acquirePermission()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(ApiResponse.<PaymentResponse>builder()
                            .status(429)
                            .description("Too many requests")
                            .errors("Payment initiation rate limit exceeded. Please try again in a moment.")
                            .build());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(paymentService.initiatePayment(request, idempotencyKey)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getPaymentStatus(id)));
    }

    @PostMapping("/{id}/webhook")
    public ResponseEntity<ApiResponse<PaymentResponse>> simulateWebhook(
            @PathVariable String id,
            @RequestBody WebhookRequest webhookRequest) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.simulateWebhook(id, webhookRequest)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getTransactionHistory() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.getTransactionHistory()));
    }
}
