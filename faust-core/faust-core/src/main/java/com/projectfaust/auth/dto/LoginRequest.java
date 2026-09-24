package com.projectfaust.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the login endpoint.
 *
 * @param email    the operator's registered email address.
 * @param password the operator's plaintext password (compared against BCrypt hash).
 * @author Dimitri / Project Faust
 */
public record LoginRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a valid email address.")
        String email,

        @NotBlank(message = "Password is required.")
        String password
) {}
