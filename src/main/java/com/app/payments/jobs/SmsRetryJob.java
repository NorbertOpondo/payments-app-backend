package com.app.payments.jobs;

import com.app.payments.integrations.SmsService;
import com.app.payments.model.SmsRecord;
import com.app.payments.repositories.SmsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsRetryJob {

    private final SmsRepository smsRepository;
    private final SmsService smsService;

    @Scheduled(fixedDelayString = "${sms.retry-job.fixed-delay}")
    public void retryFailedSms() {
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<SmsRecord> eligible = smsRepository.findEligibleForRetry(since);

        if (eligible.isEmpty()) {
            log.info("SMS retry job: no eligible records found");
            return;
        }

        log.info("SMS retry job: retrying {} failed SMS record(s)", eligible.size());
        eligible.forEach(smsService::send);
    }
}
