package com.app.payments.services;

import com.app.payments.controller.dto.SmsResponse;

import java.util.List;

public interface SmsRecordService {
    List<SmsResponse> getAllSms();
}
