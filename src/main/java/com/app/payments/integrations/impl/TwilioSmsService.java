package com.app.payments.integrations.impl;

import com.app.payments.integrations.SmsService;
import com.app.payments.model.SmsRecord;
import com.app.payments.repositories.SmsRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class TwilioSmsService implements SmsService {

    private static final int MAX_RETRY_COUNT = 5;

    private final SmsRepository smsRepository;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final String fromNumber;

    public TwilioSmsService(
            SmsRepository smsRepository,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry,
            @Value("${twilio.account-sid:mock}") String accountSid,
            @Value("${twilio.auth-token:mock}") String authToken,
            @Value("${twilio.from-number}") String fromNumber) {
        this.smsRepository = smsRepository;
        this.fromNumber = fromNumber;
        this.retry = retryRegistry.retry("sms");
        this.retry.getEventPublisher().onRetry(event ->
                log.warn("SMS retry attempt #{} — reason: {}", event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("sms");
        this.circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("SMS circuit breaker state: {} → {}", event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
        // Real: Twilio.init(accountSid, authToken);
    }

    @Override
    public void send(SmsRecord smsRecord) {
        smsRecord.setRetryCount(smsRecord.getRetryCount() + 1);
        smsRepository.save(smsRecord);

        Runnable decorated = CircuitBreaker.decorateRunnable(
                circuitBreaker,
                Retry.decorateRunnable(retry, () -> doSend(smsRecord))
        );

        try {
            decorated.run();
            smsRecord.setStatus(200);
            log.info("SMS delivered to {} for transaction {}", smsRecord.getPhoneNumber(), smsRecord.getTransactionId());
        } catch (Exception ex) {
            boolean terminal = smsRecord.getRetryCount() >= MAX_RETRY_COUNT;
            smsRecord.setStatus(terminal ? 500 : 400);
            if (terminal) {
                log.error("SMS permanently failed for transaction {} after {} attempt(s) — giving up: {}",
                        smsRecord.getTransactionId(), smsRecord.getRetryCount(), ex.getMessage());
            } else {
                log.warn("SMS failed for transaction {} (attempt {}/{}) — will retry: {}",
                        smsRecord.getTransactionId(), smsRecord.getRetryCount(), MAX_RETRY_COUNT, ex.getMessage());
            }
        }

        smsRepository.save(smsRecord);
    }

    private void doSend(SmsRecord smsRecord) {
        // Simulate Twilio API call — 20% failure rate to exercise retry/circuit-breaker paths
        if (ThreadLocalRandom.current().nextInt(100) < 20) {
            throw new RuntimeException("Simulated Twilio provider error (transient)");
        }

        // Real: Message.creator(new PhoneNumber(smsRecord.getPhoneNumber()),
        //           new PhoneNumber(fromNumber), smsRecord.getMessage()).create();

        System.out.printf("%n┌─ SMS ──────────────────────────────────────────┐%n");
        System.out.printf("│ From : %s%n", fromNumber);
        System.out.printf("│ To   : %s%n", smsRecord.getPhoneNumber());
        System.out.printf("│ Msg  : %s%n", smsRecord.getMessage());
        System.out.printf("│ TxID : %s%n", smsRecord.getTransactionId());
        System.out.printf("└────────────────────────────────────────────────┘%n%n");
    }
}
