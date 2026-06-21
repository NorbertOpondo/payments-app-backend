package com.app.payments.controller;

import com.app.payments.controller.dto.ApiResponse;
import com.app.payments.controller.dto.SmsResponse;
import com.app.payments.services.SmsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsRecordService smsRecordService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SmsResponse>>> getAllSms() {
        return ResponseEntity.ok(ApiResponse.success(smsRecordService.getAllSms()));
    }
}
