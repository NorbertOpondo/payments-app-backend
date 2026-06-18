package com.app.payments.integrations.impl;

import com.app.payments.integrations.SmsService;
import com.app.payments.model.SmsRecord;
import com.app.payments.repositories.SmsRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
            @Value("${twilio.account-sid}") String accountSid,
            @Value("${twilio.auth-token}") String authToken,
            @Value("${twilio.from-number}") String fromNumber) {
        this.smsRepository = smsRepository;
        this.fromNumber = fromNumber;
        this.retry = retryRegistry.retry("sms");
        this.retry.getEventPublisher().onRetry(event ->
                log.warn("SMS retry attempt #{} — reason: {}", event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("sms");
        this.circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("SMS circuit breaker state: {} → {}", event.getStateTransition().getFromState(), event.getStateTransition().getToState()));
        Twilio.init(accountSid, authToken);
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
            log.info("SMS sent to {} for transaction {}", smsRecord.getPhoneNumber(), smsRecord.getTransactionId());
        } catch (Exception ex) {
            log.error("SMS failed for transaction {} after {} attempt(s): {}",
                    smsRecord.getTransactionId(), smsRecord.getRetryCount(), ex.getMessage());
            smsRecord.setStatus(smsRecord.getRetryCount() >= MAX_RETRY_COUNT ? 500 : 400);
        }

        smsRepository.save(smsRecord);
    }

    private void doSend(SmsRecord smsRecord) {
        Message.creator(
                new PhoneNumber(smsRecord.getPhoneNumber()),
                new PhoneNumber(fromNumber),
                smsRecord.getMessage()
        ).create();
    }
}
