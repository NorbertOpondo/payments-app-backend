package com.app.payments.integrations;

import com.app.payments.model.SmsRecord;

public interface SmsService {
    void send(SmsRecord smsRecord);
}
