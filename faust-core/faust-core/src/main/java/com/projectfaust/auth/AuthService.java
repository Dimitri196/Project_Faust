package com.projectfaust.auth;

import com.projectfaust.auth.dto.AuthResponse;
import com.projectfaust.auth.dto.LoginRequest;
import com.projectfaust.user.User;
import com.projectfaust.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service orchestrating the authentication flow for Project Faust operators.
 *
 * <p>Validates credentials via Spring Security's {@link AuthenticationManager},
 * then generates a signed JWT token via {@link JwtService}.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    /**
     * Authenticates an operator and returns a signed JWT token.
     *
     * <p>Spring Security's {@link AuthenticationManager} handles credential
     * validation against the BCrypt hash stored in the database.</p>
     *
     * @param request the login request containing email and password.
     * @return an {@link AuthResponse} containing the token and operator metadata.
     * @throws org.springframework.security.core.AuthenticationException
     *         if credentials are invalid.
     * @throws EntityNotFoundException if the user account is not found after auth.
     */
    public AuthResponse login(LoginRequest request) {
        // Validate credentials — throws AuthenticationException if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Load user details and generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        // Embed role and clearance as extra claims for frontend use
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new EntityNotFoundException(
                        "FAUST_AUTH: User not found after authentication: " + request.email()));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole().name());
        extraClaims.put("clearance", user.getClearance().name());
        extraClaims.put("userId", user.getId().toString());

        String token = jwtService.generateToken(extraClaims, userDetails);

        log.info("FAUST_AUTH: Operator {} authenticated successfully.", request.email());

        return new AuthResponse(
                token,
                jwtService.getExpirationMs(),
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getClearance()
        );
    }
}