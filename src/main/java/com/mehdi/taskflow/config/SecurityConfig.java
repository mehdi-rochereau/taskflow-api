package com.mehdi.taskflow.config;

import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the TaskFlow API.
 *
 * <p>Configures a stateless JWT-based authentication mechanism.
 * CSRF protection is disabled as the API is stateless and does not use session cookies.
 * Method-level security is enabled via {@link EnableMethodSecurity} to support
 * {@code @PreAuthorize} annotations on service methods.</p>
 *
 * <p>Public endpoints (no JWT required):</p>
 * <ul>
 *   <li>{@code POST /api/auth/register}</li>
 *   <li>{@code POST /api/auth/login}</li>
 *   <li>{@code /swagger-ui/**}</li>
 *   <li>{@code /v3/api-docs/**}</li>
 * </ul>
 *
 * <p>All other endpoints require a valid JWT token passed as a
 * {@code Authorization: Bearer <token>} header, validated by {@link JwtFilter}.</p>
 *
 * @see JwtFilter
 * @see UserDetailsServiceImpl
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;
    private final MessageService messageService;

    /**
     * Constructs a new {@code SecurityConfig} with its required dependencies.
     *
     * @param jwtFilter          filter responsible for JWT token validation on each request
     * @param userDetailsService service for loading user details during authentication
     * @param messageService     utility component for resolving i18n messages based on the current request locale
     */
    public SecurityConfig(JwtFilter jwtFilter, UserDetailsServiceImpl userDetailsService, MessageService messageService) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.messageService = messageService;
    }

    /**
     * Configures the HTTP security filter chain.
     *
     * <p>Applies the following configuration:</p>
     * <ul>
     *   <li>CSRF disabled — not needed for stateless REST APIs</li>
     *   <li>Public routes: {@code /api/auth/**}, {@code /swagger-ui/**}, {@code /v3/api-docs/**}</li>
     *   <li>All other routes require authentication</li>
     *   <li>Session management: {@link SessionCreationPolicy#STATELESS} — no HTTP session created</li>
     *   <li>Custom {@link org.springframework.security.web.AuthenticationEntryPoint} — returns a
     *       structured {@code 401 Unauthorized} JSON response instead of the default HTML error page
     *       when an unauthenticated request reaches a protected endpoint</li>
     *   <li>{@link JwtFilter} inserted before {@link UsernamePasswordAuthenticationFilter}</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
}