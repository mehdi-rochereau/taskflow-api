package com.mehdi.taskflow.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Injecte les valeurs de @Value sans démarrer Spring
        ReflectionTestUtils.setField(jwtService, "secret",
                "ce-secret-doit-faire-au-moins-256-bits-soit-32-caracteres-minimum");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);

        userDetails = new User("mehdi", "password", Collections.emptyList());
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        // WHEN
        String token = jwtService.generateToken(userDetails);

        // THEN
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_shouldContainUsername() {
        // WHEN
        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);

        // THEN
        assertEquals("mehdi", username);
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        // GIVEN
        String token = jwtService.generateToken(userDetails);

        // WHEN
        boolean valid = jwtService.isTokenValid(token, userDetails);

        // THEN
        assertTrue(valid);
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        // GIVEN
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("autre", "password", Collections.emptyList());

        // WHEN
        boolean valid = jwtService.isTokenValid(token, otherUser);

        // THEN
        assertFalse(valid);
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        // GIVEN — expiration à 0ms = token immédiatement expiré
        ReflectionTestUtils.setField(jwtService, "expiration", 0L);
        String token = jwtService.generateToken(userDetails);

        // WHEN & THEN
        assertThrows(ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        // GIVEN
        String token = jwtService.generateToken(userDetails);

        // WHEN
        String username = jwtService.extractUsername(token);

        // THEN
        assertEquals("mehdi", username);
    }
}