package com.mypelink.backend.shared.infrastructure.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    private String secret = Base64.getEncoder().encodeToString(
            "my-super-secret-key-that-is-long-enough-for-hs256".getBytes());
    private long expiration = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", expiration);
    }

    @Test
    void generateAndValidateToken_Success() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        String token = jwtService.generateToken(userDetails, new HashMap<>());
        assertNotNull(token);

        String username = jwtService.extractUsername(token);
        assertEquals("test@test.com", username);

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_ShouldFail_WhenUserMismatch() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        UserDetails otherUser = mock(UserDetails.class);
        when(otherUser.getUsername()).thenReturn("other@test.com");

        String token = jwtService.generateToken(userDetails, new HashMap<>());

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void generateToken_WithExpiration_Success() {
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("test@test.com");

        String token = jwtService.generateToken(userDetails, new HashMap<>(), 60000L);
        assertNotNull(token);
    }
}
