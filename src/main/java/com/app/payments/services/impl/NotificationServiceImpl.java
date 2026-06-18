package com.app.payments.services.impl;

import com.app.payments.integrations.SmsService;
import com.app.payments.model.SmsRecord;
import com.app.payments.model.Transaction;
import com.app.payments.repositories.SmsRepository;
import com.app.payments.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SmsRepository smsRepository;
    private final SmsService smsService;

    @Async("smsExecutor")
    @Override
    public void notifyPaymentStatusChange(Transaction transaction) {
        log.info("Sending SMS notification for transaction {} — status: {}",
                transaction.getId(), transaction.getStatus());

        SmsRecord smsRecord = smsRepository.save(SmsRecord.builder()
                .phoneNumber(transaction.getPhoneNumber())
                .message(buildMessage(transaction))
                .transactionId(transaction.getId())
                .status(0)
                .retryCount(0)
                .build());

        smsService.send(smsRecord);
    }

    private String buildMessage(Transaction transaction) {
        String amount = transaction.getAmount().toPlainString();
        return switch (transaction.getStatus()) {
            case INITIATED -> "Your payment of KES " + amount + " has been initiated.";
            case PROCESSING -> "Your payment of KES " + amount + " is being processed.";
            case STK_PUSH_SENT -> "Your MPESA payment of KES " + amount + " is awaiting your confirmation. Please check your phone.";
            case COMPLETED -> "Your payment of KES " + amount + " was successful. Thank you!";
            case FAILED -> "Your payment of KES " + amount + " failed. Please try again or contact support.";
        };
    }
}
