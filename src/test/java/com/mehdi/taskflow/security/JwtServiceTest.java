package com.mehdi.taskflow.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import io.jsonwebtoken.security.WeakKeyException;
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
        ReflectionTestUtils.setField(jwtService, "secret",
                "this-secret-must-have-a-length-of-at-least-256-bits-so-32-characters-minimum");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
        userDetails = new User("mehdi", "password", Collections.emptyList());
    }

    @Test
    void generateToken_shouldReturnValidJwtFormat() {
        // WHEN
        String token = jwtService.generateToken(userDetails);

        // THEN
        assertNotNull(token);
        assertFalse(token.isEmpty());
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void generateToken_shouldGenerateDifferentTokens_forSameUser() {
        // WHEN
        String token1 = jwtService.generateToken(userDetails);
        ReflectionTestUtils.setField(jwtService, "expiration", 172800000L);
        String token2 = jwtService.generateToken(userDetails);

        // THEN
        assertNotEquals(token1, token2);
    }

    @Test
    void generateToken_shouldThrow_whenSecretIsTooShort() {
        // GIVEN
        ReflectionTestUtils.setField(jwtService, "secret", "too-short");

        // WHEN
        WeakKeyException ex = assertThrows(WeakKeyException.class,
                () -> jwtService.generateToken(userDetails));

        // THEN
        assertTrue(ex.getMessage().contains("The specified key byte array is"));
        assertTrue(ex.getMessage().contains("bits which is not secure enough for any JWT HMAC-SHA algorithm"));
        assertTrue(ex.getMessage().contains("MUST have a size >= 256 bits"));
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        // GIVEN
        String token = jwtService.generateToken(userDetails);

        // WHEN
        String username = jwtService.extractUsername(token);

        // THEN
        assertEquals("mehdi", username);
        assertNotNull(username);
        assertFalse(username.isEmpty());
    }

    @Test
    void extractUsername_shouldThrow_whenTokenIsMalformed() {
        // WHEN
        MalformedJwtException ex = assertThrows(MalformedJwtException.class,
                () -> jwtService.extractUsername("not.a.valid.jwt.token"));

        // THEN
        assertTrue(ex.getMessage().contains(
                "Malformed protected header JSON: Unable to deserialize: Unexpected character"));
    }

    @Test
    void extractUsername_shouldThrow_whenTokenIsExpired() {
        // GIVEN
        ReflectionTestUtils.setField(jwtService, "expiration", 0L);
        String token = jwtService.generateToken(userDetails);

        // WHEN
        ExpiredJwtException ex = assertThrows(ExpiredJwtException.class,
                () -> jwtService.extractUsername(token));

        // THEN
        assertTrue(ex.getMessage().contains("JWT expired "));
        assertTrue(ex.getMessage().contains(" milliseconds ago at "));
        assertTrue(ex.getMessage().contains(" Allowed clock skew: 0 milliseconds."));
        assertEquals("mehdi", ex.getClaims().getSubject());
    }

    @Test
    void extractUsername_shouldThrow_whenTokenSignedWithDifferentKey() {
        // GIVEN
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret",
                "other-secret-completely-different-must-be-32-characters");
        ReflectionTestUtils.setField(otherService, "expiration", 86400000L);
        String tokenFromOtherService = otherService.generateToken(userDetails);

        // WHEN
        SignatureException ex = assertThrows(SignatureException.class,
                () -> jwtService.extractUsername(tokenFromOtherService));

        // THEN
        assertEquals("JWT signature does not match locally computed signature. " +
                        "JWT validity cannot be asserted and should not be trusted.",
                ex.getMessage());
    }

    @Test
    void extractUsername_shouldThrow_whenTokenIsNull() {
        // WHEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jwtService.extractUsername(null));

        // THEN
        assertEquals("CharSequence cannot be null or empty.", ex.getMessage());
    }

    @Test
    void extractUsername_shouldThrow_whenTokenIsEmpty() {
        // WHEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jwtService.extractUsername(""));

        // THEN
        assertEquals("CharSequence cannot be null or empty.", ex.getMessage());
    }

    @Test
    void extractUsername_shouldThrow_whenTokenIsBlank() {
        // WHEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> jwtService.extractUsername("   "));

        // THEN
        assertEquals("CharSequence cannot be null or empty.", ex.getMessage());
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
        UserDetails otherUser = new User("other", "password", Collections.emptyList());

        // WHEN
        boolean valid = jwtService.isTokenValid(token, otherUser);

        // THEN
        assertFalse(valid);
    }

    @Test
    void isTokenValid_shouldThrow_whenTokenIsExpired() {
        // GIVEN
        ReflectionTestUtils.setField(jwtService, "expiration", 0L);
        String token = jwtService.generateToken(userDetails);

        // WHEN
        ExpiredJwtException ex = assertThrows(ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, userDetails));

        // THEN
        assertTrue(ex.getMessage().contains("JWT expired "));
        assertTrue(ex.getMessage().contains(" milliseconds ago at "));
        assertTrue(ex.getMessage().contains(" Allowed clock skew: 0 milliseconds."));
        assertEquals("mehdi", ex.getClaims().getSubject());
    }

    @Test
    void isTokenValid_shouldThrow_whenTokenSignedWithDifferentKey() {
        // GIVEN
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret",
                "other-secret-completely-different-must-be-32-characters");
        ReflectionTestUtils.setField(otherService, "expiration", 86400000L);
        String tokenFromOtherService = otherService.generateToken(userDetails);

        // WHEN
        SignatureException ex = assertThrows(SignatureException.class,
                () -> jwtService.isTokenValid(tokenFromOtherService, userDetails));

        // THEN
        assertEquals("JWT signature does not match locally computed signature. " +
                        "JWT validity cannot be asserted and should not be trusted.",
                ex.getMessage());
    }
}