package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.MessageService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter executed once per HTTP request.
 *
 * <p>Intercepts incoming requests and validates the JWT token carried by the {@code jwt} HttpOnly
 * cookie. If the token is valid, the authenticated user is stored in the {@link
 * SecurityContextHolder} for the duration of the request.
 *
 * <p>Filter execution flow:
 *
 * <ol>
 *   <li>Extract the {@code jwt} cookie — continue the chain untouched if absent
 *   <li>Extract the username from the JWT token
 *   <li>Load the user from the database via {@link UserDetailsServiceImpl}
 *   <li>Validate the token against the loaded user
 *   <li>Set the authentication in {@link SecurityContextHolder}
 *   <li>Continue the filter chain
 * </ol>
 *
 * <p>Error handling:
 *
 * <ul>
 *   <li>{@link ExpiredJwtException} → {@code 401} with message {@code "Token expiré"}
 *   <li>Any other exception → {@code 401} with message {@code "Token invalide"}
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
     * @param jwtService service for token validation and claim extraction
     * @param userDetailsService service for loading user details from the database
     * @param messageService utility component for resolving i18n messages based on the current
     *     request locale
     */
    public JwtFilter(
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            MessageService messageService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.messageService = messageService;
    }

    /**
     * Processes the JWT token from the request and sets the authentication context.
     *
     * <p>Extracts the JWT token from the {@code jwt} HttpOnly cookie.
     *
     * <p>Requests without that cookie are passed through unmodified — Spring Security then applies
     * the authorization rules of the endpoint, which will reject the request through the configured
     * authentication entry point if the endpoint is protected.
     *
     * <p>If the user is already authenticated in the current security context, no re-authentication
     * is performed.
     *
     * <p>Error handling — all JWT-related exceptions are caught and result in a {@code 401}
     * response:
     *
     * <ul>
     *   <li>{@link io.jsonwebtoken.ExpiredJwtException} → {@code 401} with message {@code "Token
     *       expiré"}
     *   <li>{@link io.jsonwebtoken.MalformedJwtException} → {@code 401} with message {@code "Token
     *       invalide"}
     *   <li>{@link io.jsonwebtoken.UnsupportedJwtException} → {@code 401} with message {@code
     *       "Token invalide"}
     *   <li>{@link io.jsonwebtoken.security.SignatureException} → {@code 401} with message {@code
     *       "Token invalide"}
     *   <li>{@link IllegalArgumentException} → {@code 401} with message {@code "Token invalide"}
     *   <li>{@link org.springframework.security.core.userdetails.UsernameNotFoundException} →
     *       {@code 401} with message {@code "Token invalide"} — occurs if the user no longer exists
     *       in the database
     * </ul>
     *
     * @param request the incoming HTTP request
     * @param response the HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs while writing the error response
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String jwt = extractToken(request);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"status\":401,\"message\":\""
                                    + messageService.get("error.jwt.expired")
                                    + "\"}");
            return;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write(
                            "{\"status\":401,\"message\":\""
                                    + messageService.get("error.jwt.invalid")
                                    + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT token from the {@code jwt} HttpOnly cookie.
     *
     * <p>The cookie is the only accepted transport. The {@code Authorization: Bearer} header used
     * to be read first, a leftover from the period when the token was stored client-side. Browsers,
     * Postman and curl all keep a cookie jar and send the cookie back automatically, so the header
     * carried no capability the cookie does not already provide, while keeping a token value in
     * reach of page JavaScript.
     *
     * @param request the incoming HTTP request
     * @return the JWT token string, or {@code null} if the cookie is absent
     */
    private String extractToken(HttpServletRequest request) {
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
