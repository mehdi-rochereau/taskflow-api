package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UserDetailsService} for Spring Security integration.
 *
 * <p>Loads a user from the database using either their username or email address.
 * This dual-lookup strategy supports login via both identifiers, consistent
 * with the authentication flow in {@link com.mehdi.taskflow.user.UserService}.</p>
 *
 * <p>Called by Spring Security's {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}
 * during authentication, and by {@link JwtFilter} when validating JWT tokens</p>
 *
 * @see UserDetailsService
 * @see JwtFilter
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final MessageService messageService;

    /**
     * Constructs a new {@code UserDetailsServiceImpl} with its required dependency.
     *
     * @param userRepository repository used to look up users by username or email
     * @param messageService utility component for resolving i18n messages based on the current request locale
     */
    public UserDetailsServiceImpl(UserRepository userRepository, MessageService messageService) {
        this.userRepository = userRepository;
        this.messageService = messageService;
    }

    /**
     * Loads a user by username or email address.
     *
     * <p>Extends the default {@link UserDetailsService} contract by accepting
     * either a username or an email address as identifier — first attempts
     * to find the user by username, then falls back to an email lookup.</p>
     *
     * <p>Both lookups are case-sensitive as per the underlying JPA repository
     * implementation. Ensure the identifier is provided in the exact case
     * used during registration.</p>
     *
     * @param identifier the username or email address identifying the user —
     *                   never {@code null} or empty
     * @return a fully populated {@link UserDetails} instance (never {@code null})
     * @throws UsernameNotFoundException if no user matches the provided identifier,
     *                                   or if the user has no {@link org.springframework.security.core.GrantedAuthority}
     */
    @Override
    public UserDetails loadUserByUsername(String identifier)
            throws UsernameNotFoundException {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(
                        messageService.get("error.identifier.not.found", identifier)));
    }
}