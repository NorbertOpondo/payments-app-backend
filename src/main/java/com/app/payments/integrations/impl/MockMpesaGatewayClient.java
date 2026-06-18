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
@Service("mpesaGatewayClient")
public class MockMpesaGatewayClient implements PaymentGatewayClient {

    private final RestClient restClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public MockMpesaGatewayClient(
            @Qualifier("mpesaRestClient") RestClient restClient,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restClient = restClient;
        this.retry = retryRegistry.retry("mpesa");
        this.retry.getEventPublisher().onRetry(event ->
                log.warn("MPESA retry attempt #{} — reason: {}", event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("mpesa");
        this.circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("MPESA circuit breaker state: {} → {}", event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
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
        log.info("Initiating MPESA STK push — ref: {}, phone: {}, amount: {}",
                request.getReferenceId(), request.getPhoneNumber(), request.getAmount());

        if (ThreadLocalRandom.current().nextInt(100) < 60) {
            throw new RuntimeException("Simulated MPESA gateway timeout");
        }

        return GatewayResponse.builder()
                .referenceId(request.getReferenceId())
                .status(TransactionStatus.STK_PUSH_SENT)
                .message("STK push sent to customer — awaiting confirmation")
                .build();
    }

    private GatewayResponse fallback(GatewayRequest request, Exception ex) {
        log.error("MPESA gateway unavailable — ref: {}, reason: {}", request.getReferenceId(), ex.getMessage());
        return GatewayResponse.builder()
                .referenceId(request.getReferenceId())
                .status(TransactionStatus.FAILED)
                .message("MPESA gateway unavailable, please try again later")
                .build();
    }
}
