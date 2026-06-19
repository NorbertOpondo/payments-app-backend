package com.app.payments.controller;

import com.app.payments.controller.dto.ApiResponse;
import com.app.payments.controller.dto.AuthResponse;
import com.app.payments.controller.dto.LoginRequest;
import com.app.payments.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

    @Value("${app.auth.username}")
    private String allowedUsername;

    @Value("${app.auth.password}")
    private String allowedPassword;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        if (!allowedUsername.equals(request.getUsername()) || !allowedPassword.equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
        String token = jwtService.generateToken(request.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                AuthResponse.builder()
                        .token(token)
                        .username(request.getUsername())
                        .build()
        ));
    }
}
