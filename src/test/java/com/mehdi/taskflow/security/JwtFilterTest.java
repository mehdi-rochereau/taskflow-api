package com.mehdi.taskflow.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    private UserDetails userDetails;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        userDetails = new User("mehdi", "password", Collections.emptyList());
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenNoAuthHeader() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn(null);

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenAuthHeaderIsNotBearer() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(any());
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenUsernameIsNull() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("Bearer some-token");
        when(jwtService.extractUsername("some-token")).thenReturn(null);

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSkipAuthentication_whenAlreadyAuthenticated() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("mehdi");

        // Simule un utilisateur déjà authentifié dans le contexte
        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldAuthenticateUser_whenTokenIsValid() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtService.extractUsername("valid-token")).thenReturn("mehdi");
        when(userDetailsService.loadUserByUsername("mehdi")).thenReturn(userDetails);
        when(jwtService.isTokenValid("valid-token", userDetails)).thenReturn(true);

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("mehdi", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenTokenIsInvalid() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtService.extractUsername("invalid-token")).thenReturn("mehdi");
        when(userDetailsService.loadUserByUsername("mehdi")).thenReturn(userDetails);
        when(jwtService.isTokenValid("invalid-token", userDetails)).thenReturn(false);

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldReturn401_whenTokenIsExpired() throws Exception {
        // GIVEN
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtService.extractUsername("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expiré"));

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Token expiré"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_shouldReturn401_whenTokenIsInvalidFormat() throws Exception {
        // GIVEN
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-format-token");
        when(jwtService.extractUsername("bad-format-token"))
                .thenThrow(new RuntimeException("Token invalide"));

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        assertTrue(responseWriter.toString().contains("Token invalide"));
        verify(filterChain, never()).doFilter(any(), any());
    }
}