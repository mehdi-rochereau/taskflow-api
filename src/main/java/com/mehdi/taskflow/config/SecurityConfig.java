package com.mehdi.taskflow.config;

import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.RateLimitFilter;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for the TaskFlow API.
 *
 * <p>Configures a stateless JWT-based authentication mechanism with rate limiting
 * on authentication endpoints to prevent brute force attacks.
 * CSRF protection is disabled as the API is stateless and does not use session cookies.
 * Method-level security is enabled via {@link EnableMethodSecurity} to support
 * {@code @PreAuthorize} annotations on service methods.</p>
 *
 * <p>Public endpoints (no JWT required):</p>
 * <ul>
 *   <li>{@code POST /api/auth/register}</li>
 *   <li>{@code POST /api/auth/login}</li>
 *   <li>{@code POST /api/auth/refresh}</li>
 *   <li>{@code POST /api/auth/logout}</li>
 *   <li>{@code /swagger-ui/**}</li>
 *   <li>{@code /v3/api-docs/**}</li>
 *   <li>{@code /redoc.html}</li>
 * </ul>
 *
 * <p>All other endpoints require a valid JWT token passed as a
 * {@code Authorization: Bearer <token>} header, validated by {@link JwtFilter}.</p>
 *
 * @see JwtFilter
 * @see RateLimitFilter
 * @see UserDetailsServiceImpl
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final MessageService messageService;
    private final RateLimitFilter rateLimitFilter;

    /**
     * Constructs a new {@code SecurityConfig} with its required dependencies.
     *
     * @param jwtFilter          filter responsible for JWT token validation on each request
     * @param userDetailsService service for loading user details during authentication
     * @param messageService     utility component for resolving i18n messages based on the current request locale
     * @param rateLimitFilter    filter responsible for rate limiting on authentication endpoints
     */
    public SecurityConfig(JwtFilter jwtFilter,
                          UserDetailsServiceImpl userDetailsService,
                          MessageService messageService,
                          RateLimitFilter rateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.messageService = messageService;
        this.rateLimitFilter = rateLimitFilter;

    }

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>Applies the following configuration:</p>
     * <ul>
     *   <li>CORS — allows requests from the Angular frontend on {@code localhost:4200}</li>
     *   <li>Security headers — {@code X-Frame-Options: DENY}, {@code X-Content-Type-Options: nosniff},
     *       {@code Strict-Transport-Security}, {@code Content-Security-Policy: default-src 'self'},
     *       {@code Referrer-Policy: no-referrer}</li>
     *   <li>CSRF disabled — not needed for stateless REST APIs</li>
     *   <li>Public routes: {@code /api/auth/**}, {@code /swagger-ui/**},
     *       {@code /v3/api-docs/**}, {@code /redoc.html}</li>
     *   <li>All other routes require authentication</li>
     *   <li>Session management: {@link SessionCreationPolicy#STATELESS} — no HTTP session created</li>
     *   <li>Custom {@link org.springframework.security.web.AuthenticationEntryPoint} — returns a
     *       structured {@code 401 Unauthorized} JSON response instead of the default HTML error page
     *       when an unauthenticated request reaches a protected endpoint</li>
     *   <li>{@link RateLimitFilter} inserted before {@link UsernamePasswordAuthenticationFilter}
     *       — limits login to 5 attempts/minute and registration to 3 attempts/hour per IP</li>
     *   <li>{@link JwtFilter} inserted before {@link UsernamePasswordAuthenticationFilter}
     *       — validates JWT tokens on every protected request</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(Customizer.withDefaults())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'")
                        )
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                        )
                )
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/redoc.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"status\":401,\"message\":\""
                                            + messageService.get("error.authentication.required")
                                            + "\"}"
                            );
                        })
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Configures the authentication provider used by Spring Security.
     *
     * <p>Uses a {@link DaoAuthenticationProvider} backed by {@link UserDetailsServiceImpl}
     * for user lookup and {@link BCryptPasswordEncoder} for password verification.</p>
     *
     * @return the configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean.
     *
     * <p>Required by {@link com.mehdi.taskflow.user.UserService} to authenticate
     * users during login via
     * {@link AuthenticationManager#authenticate(org.springframework.security.core.Authentication)}.</p>
     *
     * @param config the Spring Security authentication configuration
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if the {@link AuthenticationManager} cannot be retrieved
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides a BCrypt password encoder bean.
     *
     * <p>Used by {@link DaoAuthenticationProvider} to verify passwords during login,
     * and by {@link com.mehdi.taskflow.user.UserService} to encode passwords
     * before persisting new user accounts.</p>
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures CORS to allow requests from the Angular frontend.
     *
     * <p>Allows all headers and the standard HTTP methods used by the API.
     * Credentials are allowed to support JWT Bearer token transmission.</p>
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}