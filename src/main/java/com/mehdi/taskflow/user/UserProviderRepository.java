package com.mehdi.taskflow.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link UserProvider} entities.
 *
 * <p>Provides lookup methods for finding linked providers
 * by provider type and provider-specific identifier.</p>
 */
@Repository
public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {

    /**
     * Finds a linked provider by provider type and provider-specific identifier.
     *
     * @param provider   the OAuth2 provider type
     * @param providerId the unique identifier from the provider
     * @return the matching {@link UserProvider} if found
     */
    Optional<UserProvider> findByProviderAndProviderId(
            UserProvider.Provider provider, String providerId);

    /**
     * Finds a linked provider by user and provider type.
     *
     * @param user     the user account
     * @param provider the OAuth2 provider type
     * @return the matching {@link UserProvider} if found
     */
    Optional<UserProvider> findByUserAndProvider(User user, UserProvider.Provider provider);

    /**
     * Checks whether a provider is already linked to a given user.
     *
     * @param user     the user account
     * @param provider the OAuth2 provider type
     * @return {@code true} if the provider is already linked
     */
    boolean existsByUserAndProvider(User user, UserProvider.Provider provider);

    /**
     * Counts the number of providers linked to a user.
     * Used to prevent unlinking the last remaining provider.
     *
     * @param user the user account
     * @return the number of linked providers
     */
    long countByUser(User user);
}