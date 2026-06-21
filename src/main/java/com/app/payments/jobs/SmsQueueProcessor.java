package com.app.payments.jobs;

import com.app.payments.integrations.SmsService;
import com.app.payments.model.SmsRecord;
import com.app.payments.services.impl.SmsQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsQueueProcessor {

    private final SmsQueue smsQueue;
    private final SmsService smsService;

    @Scheduled(fixedDelay = 2000)
    public void process() {
        List<SmsRecord> batch = smsQueue.drain();
        if (batch.isEmpty()) return;

        log.info("SMS queue processor: dispatching {} message(s)", batch.size());
        batch.forEach(smsService::send);
    }
}
