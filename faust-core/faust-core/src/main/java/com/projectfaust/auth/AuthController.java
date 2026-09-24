package com.projectfaust.auth;

import com.projectfaust.auth.dto.AuthResponse;
import com.projectfaust.auth.dto.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints for Project Faust operators.
 *
 * <p>Base path: {@code /api/v1/auth}</p>
 *
 * <p>This endpoint is publicly accessible — it is excluded from authentication
 * requirements in {@link com.projectfaust.config.SecurityConfig}.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Operator login and token management")
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates an operator and returns a signed JWT token.
     *
     * <p>The returned token must be sent in subsequent requests as:
     * {@code Authorization: Bearer <token>}</p>
     *
     * @param request the login credentials.
     * @return the JWT token and operator profile metadata.
     */
    @PostMapping("/login")
    @Operation(summary = "Operator login",
            description = "Validates credentials and returns a signed JWT token. " +
                    "Send the token as 'Authorization: Bearer <token>' on all subsequent requests.")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}