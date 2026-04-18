package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.MessageService;
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
import java.util.ArrayList;
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

    @Mock
    private MessageService messageService;

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
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(messageService, never()).get(any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setContentType("application/json");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldContinueChain_whenAuthHeaderIsNotBearer() throws Exception {
        // GIVEN
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(jwtService, never()).extractUsername(any());
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(messageService, never()).get(any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setContentType("application/json");
        verify(filterChain).doFilter(request, response);
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
        verify(jwtService).extractUsername("some-token");
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(messageService, never()).get(any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setContentType("application/json");
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
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals("mehdi", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(new ArrayList<>(userDetails.getAuthorities()),
                new ArrayList<>(SecurityContextHolder.getContext().getAuthentication().getAuthorities()));
        verify(jwtService).extractUsername("valid-token");
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(any(), any());
        verify(messageService, never()).get(any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setContentType("application/json");
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
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());
        assertEquals(userDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals("mehdi", SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals(new ArrayList<>(userDetails.getAuthorities()),
                new ArrayList<>(SecurityContextHolder.getContext().getAuthentication().getAuthorities()));
        verify(jwtService).extractUsername("valid-token");
        verify(userDetailsService).loadUserByUsername("mehdi");
        verify(jwtService).isTokenValid("valid-token", userDetails);
        verify(messageService, never()).get(any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setContentType("application/json");
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
        verify(jwtService).extractUsername("invalid-token");
        verify(userDetailsService).loadUserByUsername("mehdi");
        verify(jwtService).isTokenValid("invalid-token", userDetails);
        verify(messageService, never()).get(any());
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response, never()).setContentType("application/json");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldReturn401_whenTokenIsExpired() throws Exception {
        // GIVEN
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getHeader("Authorization")).thenReturn("Bearer expired-token");
        when(jwtService.extractUsername("expired-token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token expired"));
        when(messageService.get("error.jwt.expired")).thenReturn("Token expired");

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("{\"status\":401,\"message\":\"Token expired\"}",
                responseWriter.toString());
        verify(jwtService).extractUsername("expired-token");
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(anyString(), any());
        verify(messageService).get("error.jwt.expired");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(messageService, never()).get("error.jwt.invalid");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_shouldReturn401_whenTokenIsInvalidFormat() throws Exception {
        // GIVEN
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-format-token");
        when(jwtService.extractUsername("bad-format-token"))
                .thenThrow(new RuntimeException("Invalid token"));
        when(messageService.get("error.jwt.invalid")).thenReturn("Invalid token");

        // WHEN
        jwtFilter.doFilterInternal(request, response, filterChain);

        // THEN
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("{\"status\":401,\"message\":\"Invalid token\"}",
                responseWriter.toString());
        verify(jwtService).extractUsername("bad-format-token");
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).isTokenValid(anyString(), any());
        verify(messageService).get("error.jwt.invalid");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json");
        verify(messageService, never()).get("error.jwt.expired");
        verify(filterChain, never()).doFilter(any(), any());
    }
}