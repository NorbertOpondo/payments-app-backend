package com.app.payments.controller;

import com.app.payments.controller.dto.ApiResponse;
import com.app.payments.controller.dto.AuthResponse;
import com.app.payments.controller.dto.LoginRequest;
import com.app.payments.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        String token = jwtService.generateToken(request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .token(token)
                        .username(request.getUsername())
                        .build()
        ));
    }
}
