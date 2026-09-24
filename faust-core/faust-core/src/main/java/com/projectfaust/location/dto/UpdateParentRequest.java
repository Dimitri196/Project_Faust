package com.projectfaust.location.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request body for the update-parent operation on a geographic node.
 *
 * <p>The new parent UUID is deliberately passed in the request body rather than
 * as a query parameter, to prevent sensitive node identifiers from appearing
 * in proxy access logs, CDN logs, or browser history.</p>
 *
 * @param newParentId the public UUID of the intended new parent node. Must not be null.
 * @author Dimitri / Project Faust
 */
public record UpdateParentRequest(
        @NotNull(message = "newParentId is required")
        UUID newParentId
) {}
