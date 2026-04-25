package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.MessageService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT authentication filter executed once per HTTP request.
 *
 * <p>Intercepts incoming requests and validates the JWT token present
 * in the {@code Authorization: Bearer <token>} header.
 * If the token is valid, the authenticated user is stored in the
 * {@link SecurityContextHolder} for the duration of the request.</p>
 *
 * <p>Filter execution flow:</p>
 * <ol>
 *   <li>Extract the {@code Authorization} header — skip if absent or not Bearer</li>
 *   <li>Extract the username from the JWT token</li>
 *   <li>Load the user from the database via {@link UserDetailsServiceImpl}</li>
 *   <li>Validate the token against the loaded user</li>
 *   <li>Set the authentication in {@link SecurityContextHolder}</li>
 *   <li>Continue the filter chain</li>
 * </ol>
 *
 * <p>Error handling:</p>
 * <ul>
 *   <li>{@link ExpiredJwtException} → {@code 401} with message {@code "Token expiré"}</li>
 *   <li>Any other exception → {@code 401} with message {@code "Token invalide"}</li>
 * </ul>
 *
 * @see JwtService
 * @see UserDetailsServiceImpl
 * @see org.springframework.web.filter.OncePerRequestFilter
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final MessageService messageService;

    /**
     * Constructs a new {@code JwtFilter} with its required dependencies.
     *
     * @param jwtService         service for token validation and claim extraction
     * @param userDetailsService service for loading user details from the database
     * @param messageService utility component for resolving i18n messages based on the current request locale
     */
    public JwtFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService, MessageService messageService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.messageService = messageService;
    }

    /**
     * Processes the JWT token from the request and sets the authentication context.
     *
     * <p>Extracts the JWT token from either the {@code Authorization: Bearer} header
     * or the {@code jwt} HttpOnly cookie — header takes precedence.</p>
     *
     * <p>Requests without a valid {@code Authorization: Bearer} header are passed
     * through without modification — Spring Security will handle authorization
     * based on the endpoint configuration.</p>
     *
     * <p>If the user is already authenticated in the current security context,
     * no re-authentication is performed.</p>
     *
     * <p>Error handling — all JWT-related exceptions are caught and result in a {@code 401} response:</p>
     * <ul>
     *   <li>{@link io.jsonwebtoken.ExpiredJwtException} → {@code 401} with message {@code "Token expiré"}</li>
     *   <li>{@link io.jsonwebtoken.MalformedJwtException} → {@code 401} with message {@code "Token invalide"}</li>
     *   <li>{@link io.jsonwebtoken.UnsupportedJwtException} → {@code 401} with message {@code "Token invalide"}</li>
     *   <li>{@link io.jsonwebtoken.security.SignatureException} → {@code 401} with message {@code "Token invalide"}</li>
     *   <li>{@link IllegalArgumentException} → {@code 401} with message {@code "Token invalide"}</li>
     *   <li>{@link org.springframework.security.core.userdetails.UsernameNotFoundException} → {@code 401}
     *       with message {@code "Token invalide"} — occurs if the user no longer exists in the database</li>
     * </ul>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs while writing the error response
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String jwt = extractToken(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\""
                    + messageService.get("error.jwt.expired") + "\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\""
                    + messageService.get("error.jwt.invalid") + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the request.
     *
     * <p>Checks the {@code Authorization: Bearer} header first,
     * then falls back to the {@code jwt} HttpOnly cookie.
     * This dual strategy allows testing via Swagger/Postman with the header
     * while using the secure cookie in production with Angular.</p>
     *
     * @param request the incoming HTTP request
     * @return the JWT token string, or {@code null} if not found
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}