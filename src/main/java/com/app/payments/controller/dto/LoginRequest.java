package com.app.payments.controller.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
