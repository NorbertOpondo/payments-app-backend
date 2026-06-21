package com.app.payments.services.impl;

import com.app.payments.controller.dto.SmsResponse;
import com.app.payments.model.SmsRecord;
import com.app.payments.repositories.SmsRepository;
import com.app.payments.services.SmsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SmsRecordServiceImpl implements SmsRecordService {

    private final SmsRepository smsRepository;

    @Override
    public List<SmsResponse> getAllSms() {
        return smsRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private SmsResponse toResponse(SmsRecord record) {
        return SmsResponse.builder()
                .id(record.getId())
                .phoneNumber(record.getPhoneNumber())
                .message(record.getMessage())
                .transactionId(record.getTransactionId())
                .status(record.getStatus())
                .retryCount(record.getRetryCount())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
