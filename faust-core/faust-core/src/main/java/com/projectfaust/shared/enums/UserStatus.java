package com.projectfaust.shared.enums;

/**
 * Defines the operational status of a Project Faust operator account.
 *
 * @author Dimitri / Project Faust
 */
public enum UserStatus {

    /**
     * Account is active and the operator can authenticate normally.
     */
    OPERATIONAL,

    /**
     * Account has been temporarily suspended — authentication is blocked.
     * The account can be reinstated by a SUPER_ADMIN.
     */
    SUSPENDED,

    /**
     * Account has been permanently deactivated. Not recoverable without
     * explicit SUPER_ADMIN action.
     */
    INACTIVE,

    /**
     * Account has been created but the operator has not yet completed
     * the activation process (e.g. first-login password change).
     */
    PENDING_ACTIVATION
}