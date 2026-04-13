package com.mehdi.taskflow.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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

    /**
     * Constructs a new {@code JwtFilter} with its required dependencies.
     *
     * @param jwtService         service for token validation and claim extraction
     * @param userDetailsService service for loading user details from the database
     */
    public JwtFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Processes the JWT token from the request and sets the authentication context.
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

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

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
            response.getWriter().write("{\"status\":401,\"message\":\"Token expiré\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"message\":\"Token invalide\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}