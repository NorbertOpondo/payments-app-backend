package com.app.payments.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-jwt-secret-key-that-is-at-least-32-bytes-long";
    private static final long EXPIRATION_MS = 3_600_000L;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
    }

    @Test
    void generateToken_returnsNonNullThreePartToken() {
        String token = jwtService.generateToken("testuser");
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractUsername_returnsCorrectSubject() {
        String token = jwtService.generateToken("norbert");
        assertThat(jwtService.extractUsername(token)).isEqualTo("norbert");
    }

    @Test
    void isTokenValid_freshToken_returnsTrue() {
        String token = jwtService.generateToken("testuser");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_randomString_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.valid.token")).isFalse();
    }

    @Test
    void isTokenValid_tamperedSignature_returnsFalse() {
        String token = jwtService.generateToken("testuser");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignaturexxx";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        String expiredToken = jwtService.generateToken("testuser");

        ReflectionTestUtils.setField(jwtService, "expirationMs", EXPIRATION_MS);
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void generateToken_differentUsersProduceDifferentTokens() {
        String aliceToken = jwtService.generateToken("alice");
        String bobToken = jwtService.generateToken("bob");
        assertThat(aliceToken).isNotEqualTo(bobToken);
    }
}
