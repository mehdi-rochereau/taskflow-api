package com.mehdi.taskflow.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entity representing an application user.
 *
 * <p>Implements {@link UserDetails} to integrate directly with Spring Security.
 * Each user has a unique username and email, a BCrypt-hashed password,
 * and a single role used for authorization.</p>
 *
 * <p>All account status flags ({@code isEnabled}, {@code isAccountNonExpired}, etc.)
 * return {@code true} by default. Extend this class or add dedicated fields
 * if fine-grained account management is required.</p>
 *
 * @see org.springframework.security.core.userdetails.UserDetails
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    /**
     * Auto-generated primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique username used for authentication.
     * Must not be blank and must be unique across all users.
     */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    /**
     * Unique email address.
     * Can also be used as login identifier.
     * Must be a valid email format and unique across all users.
     */
    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt-hashed password. Never stored or returned in plain text.
     */
    @NotBlank
    @Column(nullable = false)
    private String password;

    /**
     * User role used for authorization (e.g. {@code ROLE_USER}, {@code ROLE_ADMIN}).
     * Defaults to {@code ROLE_USER} on creation.
     */
    @Column(nullable = false)
    private String role = "ROLE_USER";

    /**
     * Timestamp of account creation. Set automatically on first persist.
     * Cannot be updated after creation.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default constructor required by JPA.
     */
    public User() {}

    /**
     * Sets the creation timestamp before the entity is first persisted.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters and setters ---

    /** @return the user's unique identifier */
    public Long getId() { return id; }

    /** @param id the user's unique identifier */
    public void setId(Long id) { this.id = id; }

    /** @return the user's email address */
    public String getEmail() { return email; }

    /** @param email the user's email address */
    public void setEmail(String email) { this.email = email; }

    /** @param username the unique username */
    public void setUsername(String username) { this.username = username; }

    /** @return the user's role */
    public String getRole() { return role; }

    /** @param role the user's role */
    public void setRole(String role) { this.role = role; }

    /** @param password the BCrypt-hashed password */
    public void setPassword(String password) { this.password = password; }

    /** @return the account creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }

    /** @param createdAt the account creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // --- UserDetails implementation ---

    /**
     * Returns the username used to authenticate the user.
     *
     * @return the unique username
     */
    @Override
    public String getUsername() { return username; }

    /**
     * Returns the BCrypt-hashed password used to authenticate the user.
     *
     * @return the hashed password
     */
    @Override
    public String getPassword() { return password; }

    /**
     * Returns the authorities granted to the user.
     * Maps the single {@link #role} field to a {@link SimpleGrantedAuthority}.
     *
     * @return a singleton list containing the user's role as a {@link GrantedAuthority}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    /**
     * Indicates whether the user's account has expired.
     * Always returns {@code true} — account expiration is not implemented.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonExpired() { return true; }

    /**
     * Indicates whether the user is locked or unlocked.
     * Always returns {@code true} — account locking is not implemented.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAccountNonLocked() { return true; }

    /**
     * Indicates whether the user's credentials have expired.
     * Always returns {@code true} — credential expiration is not implemented.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /**
     * Indicates whether the user is enabled or disabled.
     * Always returns {@code true} — account disabling is not implemented.
     *
     * @return {@code true}
     */
    @Override
    public boolean isEnabled() { return true; }
}