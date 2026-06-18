package com.app.payments.integrations.impl;

import com.app.payments.model.Transaction;
import com.app.payments.model.TransactionStatus;
import com.app.payments.repositories.PaymentRepository;
import com.app.payments.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class MpesaCallbackSimulator {

    private final PaymentRepository paymentRepository;
    private final NotificationService notificationService;

    @Async("smsExecutor")
    public void simulate(String transactionId) {
        try {
            // Simulate customer interaction delay: 4–9 seconds
            long delay = 4000 + ThreadLocalRandom.current().nextInt(5000);
            log.info("MPESA callback: waiting {}ms for customer response on transaction {}", delay, transactionId);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Transaction transaction = paymentRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            log.warn("MPESA callback: transaction {} not found", transactionId);
            return;
        }

        // 70% success, 30% failure
        TransactionStatus outcome = ThreadLocalRandom.current().nextInt(100) < 70
                ? TransactionStatus.COMPLETED
                : TransactionStatus.FAILED;

        transaction.setStatus(outcome);
        Transaction saved = paymentRepository.save(transaction);

        log.info("MPESA callback received for transaction {} — outcome: {}", transactionId, outcome);
        notificationService.notifyPaymentStatusChange(saved);
    }
}
