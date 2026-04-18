package com.mehdi.taskflow.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a linked OAuth2 provider for a user account.
 *
 * <p>A user can have multiple providers linked to their account,
 * allowing them to sign in via different OAuth2 providers
 * (e.g. GitHub, Google) or via a classic username/password (LOCAL).</p>
 *
 * <p>Each provider is uniquely identified by the combination of
 * {@code provider} and {@code providerId} — ensuring no two accounts
 * can be linked to the same OAuth2 identity.</p>
 *
 * <p>Each user can have at most one entry per provider type.</p>
 *
 * @see User
 * @see UserProviderRepository
 */
@Entity
@Table(
        name = "user_providers",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(columnNames = {"user_id", "provider"})
        }
)
public class UserProvider {

    /**
     * Supported authentication providers.
     */
    public enum Provider {
        /** Classic username/password authentication. */
        LOCAL,
        /** GitHub OAuth2 authentication. */
        GITHUB,
        /** Google OAuth2 authentication. */
        GOOGLE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user this provider is linked to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The authentication provider type.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    /**
     * The unique identifier returned by the OAuth2 provider.
     * For LOCAL accounts this is the username.
     */
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    /**
     * The email address returned by the OAuth2 provider at the time of linking.
     * May differ from the user's main email if the user changed their email
     * on the provider side.
     */
    @Column(name = "provider_email")
    private String providerEmail;

    /**
     * Timestamp when this provider was linked to the account.
     */
    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    /**
     * Sets the {@code linkedAt} timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.linkedAt = LocalDateTime.now();
    }

    /** @return the unique identifier */
    public Long getId() { return id; }

    /** @return the linked user */
    public User getUser() { return user; }

    /** @param user the linked user */
    public void setUser(User user) { this.user = user; }

    /** @return the provider type */
    public Provider getProvider() { return provider; }

    /** @param provider the provider type */
    public void setProvider(Provider provider) { this.provider = provider; }

    /** @return the provider-specific user identifier */
    public String getProviderId() { return providerId; }

    /** @param providerId the provider-specific user identifier */
    public void setProviderId(String providerId) { this.providerId = providerId; }

    /** @return the email address from the provider */
    public String getProviderEmail() { return providerEmail; }

    /** @param providerEmail the email address from the provider */
    public void setProviderEmail(String providerEmail) { this.providerEmail = providerEmail; }

    /** @return the timestamp when this provider was linked */
    public LocalDateTime getLinkedAt() { return linkedAt; }
}