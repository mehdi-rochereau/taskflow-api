// src/main/java/com/mehdi/taskflow/user/dto/AuthResponse.java
package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO returned after successful authentication (register, login or refresh).
 *
 * <p>Carries the authenticated user's identity only. The JWT itself is never exposed here: it is
 * written to the {@code jwt} HttpOnly cookie by {@link com.mehdi.taskflow.config.CookieUtils},
 * scoped to {@code /api} and valid for 15 minutes. A token placed in a response body ends up in
 * access logs, proxy caches and screenshots, which is exactly what the HttpOnly cookie is there to
 * prevent.
 *
 * @see com.mehdi.taskflow.auth.AuthController
 * @see com.mehdi.taskflow.user.UserService
 */
@Schema(
        name = "AuthResponse",
        description =
                "Response returned after successful registration or login. "
                        + "Authentication itself travels in the jwt and refreshToken HttpOnly cookies.")
public class AuthResponse {

    /** Username of the authenticated user. */
    @Schema(description = "Username of the authenticated user.", example = "mehdi")
    private String username;

    /** Email address of the authenticated user. */
    @Schema(description = "Email address of the authenticated user.", example = "mehdi@example.com")
    private String email;

    /** Default constructor required for JSON deserialization. */
    public AuthResponse() {}

    /**
     * Constructs a fully populated authentication response.
     *
     * @param username the authenticated user's username
     * @param email the authenticated user's email address
     */
    public AuthResponse(String username, String email) {
        this.username = username;
        this.email = email;
    }

    /**
     * @return the authenticated user's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the authenticated user's username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the authenticated user's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the authenticated user's email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
