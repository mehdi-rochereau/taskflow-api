package com.mehdi.taskflow.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO carrying user registration data.
 *
 * <p>Used as the request body for {@code POST /api/auth/register}.
 * All fields are validated before processing — see constraint annotations for details.</p>
 *
 * @see com.mehdi.taskflow.auth.AuthController#register(RegisterRequest)
 * @see com.mehdi.taskflow.user.UserService#register(RegisterRequest)
 */
public class RegisterRequest {

    /**
     * Unique username for the new account.
     * Must be between 3 and 50 characters and must not be blank.
     */
    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    @Size(min = 3, max = 50)
    private String username;

    /**
     * Email address for the new account.
     * Must be a valid email format and must not be blank.
     */
    @Email(message = "Email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    /**
     * Plain-text password for the new account.
     * Must be at least 8 characters. Encoded with BCrypt before persistence.
     */
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit faire au moins 8 caractères")
    private String password;

    /**
     * Default constructor required for JSON deserialization.
     */
    public RegisterRequest() {}

    /** @return the requested username */
    public String getUsername() { return username; }

    /** @param username the requested username */
    public void setUsername(String username) { this.username = username; }

    /** @return the email address */
    public String getEmail() { return email; }

    /** @param email the email address */
    public void setEmail(String email) { this.email = email; }

    /** @return the plain-text password */
    public String getPassword() { return password; }

    /** @param password the plain-text password */
    public void setPassword(String password) { this.password = password; }
}