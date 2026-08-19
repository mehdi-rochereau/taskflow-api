package com.mehdi.taskflow.config;

import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.RateLimitFilter;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Spring Security configuration for the TaskFlow API.
 *
 * <p>Configures a stateless JWT-based authentication mechanism with rate limiting on authentication
 * endpoints to prevent brute force attacks. CSRF protection is disabled: the JWT travels in a
 * cookie, but that cookie is scoped with {@code SameSite=Strict}, so a third-party site cannot make
 * the browser attach it to a cross-site request, and the CORS policy enumerates the two origins
 * allowed to call the API with credentials. No server-side session is created either, so there is
 * no session fixation surface to protect. Method-level security is enabled via {@link
 * EnableMethodSecurity} to support {@code @PreAuthorize} annotations on service methods.
 *
 * <p>Public endpoints (no JWT required):
 *
 * <ul>
 *   <li>{@code POST /api/auth/register}
 *   <li>{@code POST /api/auth/login}
 *   <li>{@code POST /api/auth/refresh}
 *   <li>{@code POST /api/auth/logout}
 *   <li>{@code /swagger-ui/**}
 *   <li>{@code /v3/api-docs/**}
 * </ul>
 *
 * <p>All other endpoints require a valid JWT token carried by the {@code jwt} HttpOnly cookie,
 * validated by {@link JwtFilter}.
 *
 * @see JwtFilter
 * @see RateLimitFilter
 * @see UserDetailsServiceImpl
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final MessageService messageService;
    private final RateLimitFilter rateLimitFilter;

    /**
     * Constructs a new {@code SecurityConfig} with its required dependencies.
     *
     * @param jwtFilter filter responsible for JWT token validation on each request
     * @param userDetailsService service for loading user details during authentication
     * @param messageService utility component for resolving i18n messages based on the current
     *     request locale
     * @param rateLimitFilter filter responsible for rate limiting on authentication endpoints
     */
    public SecurityConfig(
            JwtFilter jwtFilter,
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
     * <p>Applies the following configuration:
     *
     * <ul>
     *   <li>CORS — allowed origins loaded from the {@code cors.allowed-origins} property,
     *       configured via the {@code CORS_ALLOWED_ORIGINS} environment variable. Credentials are
     *       allowed so the browser attaches the authentication cookies
     *   <li>Security headers — {@code X-Frame-Options: DENY}, {@code X-Content-Type-Options:
     *       nosniff}, {@code Strict-Transport-Security}, {@code Content-Security-Policy:
     *       default-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;
     *       frame-ancestors 'none'}, {@code Referrer-Policy: no-referrer}
     *   <li>CSRF disabled — see the class Javadoc for the rationale
     *   <li>Public routes: {@code /api/auth/**}, {@code /swagger-ui/**}, {@code /v3/api-docs/**}
     *   <li>All other routes require authentication
     *   <li>Session management: {@link SessionCreationPolicy#STATELESS} — no HTTP session created
     *   <li>Custom {@link org.springframework.security.web.AuthenticationEntryPoint} — returns a
     *       structured {@code 401 Unauthorized} JSON response instead of the default HTML error
     *       page when an unauthenticated request reaches a protected endpoint
     *   <li>{@link RateLimitFilter} inserted before {@link UsernamePasswordAuthenticationFilter} —
     *       limits login to 5 attempts/minute and registration to 3 attempts/hour per IP
     *   <li>{@link JwtFilter} inserted before {@link UsernamePasswordAuthenticationFilter} —
     *       validates the {@code jwt} cookie on every protected request
     * </ul>
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(
                        headers ->
                                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                                        .contentTypeOptions(Customizer.withDefaults())
                                        .httpStrictTransportSecurity(
                                                hsts ->
                                                        hsts.includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000))
                                        .contentSecurityPolicy(
                                                csp ->
                                                        csp.policyDirectives(
                                                                "default-src 'self'; "
                                                                        + "style-src 'self' 'unsafe-inline'; "
                                                                        + "img-src 'self' data:; "
                                                                        + "frame-ancestors 'none'"))
                                        .referrerPolicy(
                                                referrer ->
                                                        referrer.policy(
                                                                ReferrerPolicyHeaderWriter
                                                                        .ReferrerPolicy
                                                                        .NO_REFERRER)))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/api/auth/**",
                                                "/swagger-ui/**",
                                                "/v3/api-docs/**",
                                                "/actuator/health",
                                                "/favicon.ico")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex ->
                                ex.authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                            response.setContentType("application/json");
                                            response.getWriter()
                                                    .write(
                                                            "{\"status\":401,\"message\":\""
                                                                    + messageService.get(
                                                                            "error.authentication.required")
                                                                    + "\"}");
                                        }))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Configures the authentication provider used by Spring Security.
     *
     * <p>Uses a {@link DaoAuthenticationProvider} backed by {@link UserDetailsServiceImpl} for user
     * lookup and {@link BCryptPasswordEncoder} for password verification.
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
     * <p>Required by {@link com.mehdi.taskflow.user.UserService} to authenticate users during login
     * via {@link
     * AuthenticationManager#authenticate(org.springframework.security.core.Authentication)}.
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
     * <p>Used by {@link DaoAuthenticationProvider} to verify passwords during login, and by {@link
     * com.mehdi.taskflow.user.UserService} to encode passwords before persisting new user accounts.
     *
     * @return a {@link BCryptPasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures CORS for the Angular frontend and the Swagger UI.
     *
     * <p>Allowed origins are read from the {@code cors.allowed-origins} property, bound to the
     * {@code CORS_ALLOWED_ORIGINS} environment variable. The value is a comma-separated list and
     * must be split before being handed to {@link CorsConfiguration#setAllowedOrigins(List)}:
     * wrapping the raw string in a single-element list would compare the whole property value
     * against a single {@code Origin} header, which can never match once a second origin is
     * declared.
     *
     * <p>Two origins are expected in production: the frontend, and the API's own origin. Swagger UI
     * is served from the API host, so its {@code Try it out} requests carry {@code Origin:
     * https://api.taskflow.mehdi-rochereau.dev}. Since Spring Framework 5.3 the presence of that
     * header makes the request a CORS request even when origin and target share the same host.
     *
     * <p>Credentials are allowed so that the HttpOnly {@code jwt} and {@code refreshToken} cookies
     * are transmitted. This forbids the {@code *} wildcard on origins, which is why they are
     * enumerated.
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Split on commas and trim: the property arrives as one string, and a
        // stray space around a separator would silently break the match.
        config.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(","))
                        .map(String::trim)
                        .filter(origin -> !origin.isEmpty())
                        .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/v3/api-docs/**", config);
        return source;
    }
}
