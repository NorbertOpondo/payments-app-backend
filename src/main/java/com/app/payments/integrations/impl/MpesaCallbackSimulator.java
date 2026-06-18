package com.app.payments.integrations.impl;

import com.app.payments.model.Transaction;
import com.app.payments.model.TransactionStatus;
import com.app.payments.repositories.PaymentRepository;
import com.app.payments.services.NotificationService;
import com.app.payments.utils.MaskingUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
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

        boolean success = ThreadLocalRandom.current().nextInt(100) < 70;
        TransactionStatus outcome = success ? TransactionStatus.COMPLETED : TransactionStatus.FAILED;

        Map<String, String> callback = buildCallback(success, transaction);
        transaction.setStatus(outcome);
        transaction.setReceiptNumber(callback.get("MpesaReceiptNumber"));
        try {
            transaction.setMetadata(new ObjectMapper().writeValueAsString(callback));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize callback metadata for transaction {}", transactionId, e);
        }

        Transaction saved = paymentRepository.save(transaction);
        log.info("MPESA callback received for transaction {} — outcome: {}", transactionId, outcome);
        notificationService.notifyPaymentStatusChange(saved);
    }

    private Map<String, String> buildCallback(boolean success, Transaction transaction) {
        Map<String, String> callback = new LinkedHashMap<>();
        if (success) {
            callback.put("ResultCode", "0");
            callback.put("ResultDesc", "The service request is processed successfully.");
            callback.put("MpesaReceiptNumber", MaskingUtils.generateReceiptNumber("LGR"));
            callback.put("TransactionDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            callback.put("Amount", transaction.getAmount().toPlainString());
            callback.put("PhoneNumber", transaction.getPhoneNumber());
        } else {
            callback.put("ResultCode", "1032");
            callback.put("ResultDesc", "Request cancelled by user");
        }
        return callback;
    }
}
