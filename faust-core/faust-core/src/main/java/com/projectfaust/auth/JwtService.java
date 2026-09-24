package com.projectfaust.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JWT token generation, validation, and claims extraction.
 *
 * <p>Uses HMAC-SHA256 signing with a secret key configured via
 * {@code faust.jwt.secret} in {@code application.yml}.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
public class JwtService {

    @Value("${faust.jwt.secret}")
    private String secret;

    @Value("${faust.jwt.expiration-ms}")
    private long expirationMs;

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a signed JWT token for the given user.
     *
     * @param userDetails the authenticated user.
     * @return a signed JWT token string.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a signed JWT token with additional claims.
     *
     * @param extraClaims additional claims to embed in the token.
     * @param userDetails the authenticated user.
     * @return a signed JWT token string.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Returns the configured token expiration in milliseconds.
     *
     * @return expiration duration in milliseconds.
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    // -------------------------------------------------------------------------
    // Token validation
    // -------------------------------------------------------------------------

    /**
     * Validates a JWT token against the given user details.
     *
     * @param token       the JWT token to validate.
     * @param userDetails the user to validate against.
     * @return {@code true} if the token is valid and not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // -------------------------------------------------------------------------
    // Claims extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts the username (subject) from the token.
     *
     * @param token the JWT token.
     * @return the username embedded in the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the token using a resolver function.
     *
     * @param token          the JWT token.
     * @param claimsResolver function to extract the desired claim.
     * @return the resolved claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}