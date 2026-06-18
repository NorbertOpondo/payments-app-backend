package com.app.payments.services;

import com.app.payments.model.Transaction;

public interface NotificationService {
    void notifyPaymentStatusChange(Transaction transaction);
}
