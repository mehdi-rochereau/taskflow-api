package com.mehdi.taskflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * Service responsible for JWT token generation and validation.
 *
 * <p>Uses the HMAC-SHA512 algorithm to sign tokens with a secret key
 * configured in {@code application.yml} under {@code application.jwt.secret}.
 * Token expiration is configurable via {@code application.jwt.expiration}
 * (in milliseconds, defaults to 86400000 = 24 hours).</p>
 *
 * <p>Token structure follows the standard JWT format:
 * {@code header.payload.signature}, where the payload contains
 * the username as subject and the issued-at / expiration timestamps.</p>
 *
 * @see JwtFilter
 */
@Service
public class JwtService {

    /**
     * Secret key used to sign and verify JWT tokens.
     * Must be at least 256 bits (32 characters) for HMAC-SHA256 compliance.
     * Injected from {@code application.jwt.secret}.
     */
    @Value("${application.jwt.secret}")
    private String secret;

    /**
     * Token validity duration in milliseconds.
     * Injected from {@code application.jwt.expiration}.
     */
    @Value("${application.jwt.expiration}")
    private long expiration;

    /**
     * Generates a signed JWT token for the given user.
     *
     * <p>The token payload contains:
     * <ul>
     *   <li>{@code sub} — the username</li>
     *   <li>{@code iat} — issued-at timestamp</li>
     *   <li>{@code exp} — expiration timestamp (now + configured expiration)</li>
     * </ul>
     * </p>
     *
     * <p>The signing algorithm is selected automatically based on the key length —
     * see {@link Keys#hmacShaKeyFor(byte[])} and
     * {@link io.jsonwebtoken.JwtBuilder#signWith(java.security.Key)}.</p>
     *
     * @param userDetails the authenticated user for whom to generate the token
     * @return a compact, URL-safe JWT string
     * @throws io.jsonwebtoken.security.InvalidKeyException if the signing key is insufficient or unsupported
     * @throws io.jsonwebtoken.security.WeakKeyException    if the secret is shorter than 256 bits (32 characters)
     */
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validates a JWT token against the given user.
     *
     * <p>A token is considered valid if:
     * <ul>
     *   <li>The username extracted from the token matches the provided {@link UserDetails}</li>
     *   <li>The token has not expired</li>
     * </ul>
     * </p>
     *
     * @param token       the JWT token to validate
     * @param userDetails the user to validate the token against
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extracts the username (subject claim) from a JWT token.
     *
     * @param token the JWT token
     * @return the username stored in the token's subject claim
     * @throws io.jsonwebtoken.ExpiredJwtException         if the token has expired
     * @throws io.jsonwebtoken.MalformedJwtException       if the token is incorrectly constructed
     * @throws io.jsonwebtoken.UnsupportedJwtException     if the token is not a signed Claims JWT
     * @throws io.jsonwebtoken.security.SignatureException if the token signature cannot be verified
     * @throws SecurityException                           if decryption fails (JWE only)
     * @throws IllegalArgumentException                    if the token string is null, empty or whitespace
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Checks whether a JWT token has expired.
     *
     * @param token the JWT token to check
     * @return {@code true} if the token expiration date is before the current time
     * @throws io.jsonwebtoken.ExpiredJwtException         if the token has expired
     * @throws io.jsonwebtoken.MalformedJwtException       if the token is incorrectly constructed
     * @throws io.jsonwebtoken.UnsupportedJwtException     if the token is not a signed Claims JWT
     * @throws io.jsonwebtoken.security.SignatureException if the token signature cannot be verified
     * @throws SecurityException                           if decryption fails (JWE only)
     * @throws IllegalArgumentException                    if the token string is null, empty or whitespace
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Extracts a specific claim from a JWT token using the provided resolver function.
     *
     * <p>Parses and cryptographically verifies the token signature via
     * {@link io.jsonwebtoken.JwtParser#parseSignedClaims(CharSequence)}
     * before extracting the payload. This method is the central entry point
     * for all claim extraction operations.</p>
     *
     * @param <T>            the type of the claim value to extract
     * @param token          the JWT token to parse
     * @param claimsResolver a function that extracts the desired claim from the {@link Claims} payload
     * @return the extracted claim value
     * @throws io.jsonwebtoken.ExpiredJwtException         if the token has expired
     * @throws io.jsonwebtoken.MalformedJwtException       if the token is incorrectly constructed
     * @throws io.jsonwebtoken.UnsupportedJwtException     if the token is not a signed Claims JWT
     * @throws io.jsonwebtoken.security.SignatureException if the token signature cannot be verified
     * @throws SecurityException                           if decryption fails (JWE only)
     * @throws IllegalArgumentException                    if the token string is null, empty or whitespace
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    /**
     * Builds an HMAC-SHA {@link SecretKey} from the configured secret.
     *
     * <p>Converts the secret string to a byte array using UTF-8 encoding to ensure
     * consistent behavior across all operating systems and JVM implementations.
     * The algorithm selected depends on the key length:</p>
     * <ul>
     *   <li>≥ 512 bits → HmacSHA512</li>
     *   <li>≥ 384 bits → HmacSHA384</li>
     *   <li>≥ 256 bits → HmacSHA256</li>
     * </ul>
     *
     * <p>The secret configured in {@code application.jwt.secret} must be at least
     * 256 bits (32 characters) to comply with
     * <a href="https://tools.ietf.org/html/rfc7518#section-3.2">RFC 7518, Section 3.2</a>.</p>
     *
     * @return the {@link SecretKey} used for signing and verifying JWT tokens
     * @throws io.jsonwebtoken.security.WeakKeyException if the secret is shorter than 256 bits (32 characters)
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}