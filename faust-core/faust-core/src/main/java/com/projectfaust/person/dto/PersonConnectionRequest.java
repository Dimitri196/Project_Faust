package com.projectfaust.person.dto;

import com.projectfaust.shared.enums.ConnectionType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a directed connection between two persons
 * in the Project Faust intelligence network.
 *
 * <p>Connections are the edges of the HUMINT graph. The {@code influenceScore}
 * represents the edge weight (0.0–1.0). If not provided, the service defaults
 * to the connection type's {@link ConnectionType#getDefaultWeight()}.</p>
 *
 * @param sourcePersonId  public UUID of the person initiating the relationship.
 * @param targetPersonId  public UUID of the person this relationship points to.
 * @param connectionType            the nature of the relationship (required).
 * @param influenceScore  edge weight between 0.0 and 1.0; null defers to type default.
 * @param description     free-text description of the connection context.
 * @param startDate       date from which the relationship is known to exist.
 * @param endDate         date the relationship ended; null if still active.
 * @param verificationStatus provenance state; defaults to PENDING_REVIEW in service.
 * @author Dimitri / Project Faust
 */
public record PersonConnectionRequest(

        @NotNull(message = "Source person ID is required.")
        UUID sourcePersonId,

        @NotNull(message = "Target person ID is required.")
        UUID targetPersonId,

        @NotNull(message = "Connection type is required.")
        ConnectionType connectionType,

        @DecimalMin(value = "0.0", message = "Influence score must be >= 0.0.")
        @DecimalMax(value = "1.0", message = "Influence score must be <= 1.0.")
        Double influenceScore,

        @Size(max = 2000, message = "Description must not exceed 2000 characters.")
        String description,

        LocalDate startDate,
        LocalDate endDate,
        VerificationStatus verificationStatus

) {}