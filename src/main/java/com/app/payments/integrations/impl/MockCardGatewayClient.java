package com.app.payments.integrations.impl;

import com.app.payments.integrations.PaymentGatewayClient;
import com.app.payments.integrations.dto.GatewayRequest;
import com.app.payments.integrations.dto.GatewayResponse;
import com.app.payments.model.TransactionStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Service("cardGatewayClient")
public class MockCardGatewayClient implements PaymentGatewayClient {

    private final RestClient restClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public MockCardGatewayClient(
            @Qualifier("cardRestClient") RestClient restClient,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restClient = restClient;
        this.retry = retryRegistry.retry("card");
        this.retry.getEventPublisher().onRetry(event ->
                log.warn("Card retry attempt #{} — reason: {}", event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("card");
        this.circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Card circuit breaker state: {} → {}", event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
    }

    @Override
    public GatewayResponse processPayment(GatewayRequest request) {
        Supplier<GatewayResponse> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, () -> doProcessPayment(request))
        );
        try {
            return decorated.get();
        } catch (Exception ex) {
            return fallback(request, ex);
        }
    }

    private GatewayResponse doProcessPayment(GatewayRequest request) {
        log.info("Processing card payment — ref: {}, amount: {}",
                request.getReferenceId(), request.getAmount());

        if (ThreadLocalRandom.current().nextInt(100) < 60) {
            throw new RuntimeException("Simulated card gateway timeout");
        }

        // Real implementation: restClient.post().uri("/v1/charges").body(request).retrieve().body(GatewayResponse.class)
        return GatewayResponse.builder()
                .referenceId(request.getReferenceId())
                .status(TransactionStatus.COMPLETED)
                .message("Card payment processed successfully")
                .build();
    }

    private GatewayResponse fallback(GatewayRequest request, Exception ex) {
        log.error("Card gateway unavailable — ref: {}, reason: {}", request.getReferenceId(), ex.getMessage());
        return GatewayResponse.builder()
                .referenceId(request.getReferenceId())
                .status(TransactionStatus.FAILED)
                .message("Card gateway unavailable, please try again later")
                .build();
    }
}
