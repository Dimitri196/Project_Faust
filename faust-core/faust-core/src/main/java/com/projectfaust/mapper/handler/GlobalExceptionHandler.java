package com.projectfaust.mapper.handler;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global interceptor for handling exceptions across the Faust Intelligence API.
 * Standardizes error responses using the RFC 7807 Problem Details specification.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles cases where a requested entity (Subject, Institution, or Node)
     * does not exist in the registry.
     *
     * @param ex The triggered EntityNotFoundException.
     * @return A 404 Not Found response with diagnostic details.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Handles logic violations, specifically preventing structural corruption
     * such as circular references within the institutional hierarchy.
     *
     * @param ex The triggered IllegalStateException.
     * @return A 400 Bad Request response indicating a hierarchy validation failure.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(IllegalStateException ex) {
        // Specifically targets structural integrity violations (e.g., A reports to B, B reports to A)
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Hierarchy Validation Error");
        return ResponseEntity.of(problem).build();
    }

    /**
     * Final safety net for unhandled system exceptions.
     * Logs the full stack trace for internal audit while shielding the user from sensitive details.
     *
     * @param ex The root Exception.
     * @return A 500 Internal Server Error response.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(Exception ex) {
        log.error("Critical System Failure: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred. Detailed logs have been recorded for analysis."
        );
        problem.setTitle("Internal System Error");
        return ResponseEntity.of(problem).build();
    }
}
