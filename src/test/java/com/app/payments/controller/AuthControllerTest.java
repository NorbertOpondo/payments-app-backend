package com.app.payments.controller;

import com.app.payments.controller.dto.LoginRequest;
import com.app.payments.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.payments.configs.SecurityConfig;
import com.app.payments.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean JwtService jwtService;

    @Test
    void login_returnsTokenAndUsername() throws Exception {
        given(jwtService.generateToken("norbert")).willReturn("signed.jwt.token");

        LoginRequest request = new LoginRequest();
        request.setUsername("norbert");
        request.setPassword("any-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.data.username").value("norbert"));
    }

    @Test
    void login_acceptsAnyCredentials() throws Exception {
        given(jwtService.generateToken("alice")).willReturn("alice.jwt.token");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong-password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("alice.jwt.token"));
    }

    @Test
    void login_noAuthHeaderRequired() throws Exception {
        given(jwtService.generateToken("guest")).willReturn("guest.token");

        LoginRequest request = new LoginRequest();
        request.setUsername("guest");
        request.setPassword("pw");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
