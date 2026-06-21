package com.app.payments.services.impl;

import com.app.payments.model.SmsRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class SmsQueue {

    private static final int CAPACITY = 500;

    private final LinkedBlockingQueue<SmsRecord> queue = new LinkedBlockingQueue<>(CAPACITY);

    public void enqueue(SmsRecord record) {
        if (!queue.offer(record)) {
            log.warn("SMS queue full (capacity {}), dropping message for transaction {}",
                    CAPACITY, record.getTransactionId());
        }
    }

    public List<SmsRecord> drain() {
        List<SmsRecord> batch = new ArrayList<>();
        queue.drainTo(batch);
        return batch;
    }

    public int size() {
        return queue.size();
    }
}
