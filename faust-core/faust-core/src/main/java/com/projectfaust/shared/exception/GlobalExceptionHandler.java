package com.projectfaust.shared.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for the Project Faust REST API.
 *
 * <p>Standardises all error responses using the RFC 7807 Problem Details
 * specification ({@link ProblemDetail}). Every exception type maps to a
 * specific HTTP status and a structured response body the frontend can
 * parse predictably.</p>
 *
 * <p><b>Handler precedence:</b> Spring picks the most specific handler.
 * The general {@link Exception} catch-all fires only when no more specific
 * handler matches.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Maps {@link EntityNotFoundException} to HTTP 404.
     * Fired when a requested person, institution, occupation, or other
     * entity does not exist in the registry.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Maps {@link ClearanceInsufficientException} to HTTP 403.
     * Fired when a person's security clearance is below the level required
     * to be appointed to a position.
     */
    @ExceptionHandler(ClearanceInsufficientException.class)
    public ResponseEntity<ProblemDetail> handleClearanceInsufficient(
            ClearanceInsufficientException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Clearance Insufficient");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Maps {@link BadCredentialsException} to HTTP 401.
     * Fired during login when the supplied email or password does not match.
     * Returns a generic message — never reveals whether the email exists.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        problem.setTitle("Authentication Failed");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Maps {@link IllegalStateException} to HTTP 400.
     * Fired for structural integrity violations such as circular hierarchy
     * references (A reports to B, B reports to A).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Hierarchy Validation Error");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Maps {@link IllegalArgumentException} to HTTP 400.
     * Fired for invalid input values — e.g. blank contact values in
     * {@link com.projectfaust.person.PersonService#normalizeContactValue}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Input");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Maps {@link MethodArgumentNotValidException} to HTTP 400.
     * Fired when {@code @Valid} validation fails on a request DTO.
     * Collects all field-level error messages into the detail string.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Failed");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Catch-all handler for unexpected exceptions — HTTP 500.
     * Logs the full stack trace internally while returning a generic
     * message to the client to avoid leaking implementation details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex) {
        log.error("FAUST_SYSTEM: Unhandled exception: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Logs have been recorded for analysis."
        );
        problem.setTitle("Internal System Error");
        return ResponseEntity.of(problem).build();
    }
}