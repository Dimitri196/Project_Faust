package com.projectfaust.shared.exception;

/**
 * Thrown when a person's security clearance level is insufficient
 * for the position they are being assigned to.
 *
 * <p>This is a domain exception — it represents a deliberate business rule
 * violation rather than a system error. It should be mapped to HTTP 403
 * by the global exception handler.</p>
 *
 * @author Dimitri / Project Faust
 */
public class ClearanceInsufficientException extends RuntimeException {

    public ClearanceInsufficientException(String message) {
        super(message);
    }
}