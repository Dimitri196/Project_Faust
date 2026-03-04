package com.projectfaust.repository;

import com.projectfaust.entity.ExternalContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for managing external contract records ingested from third-party APIs.
 * Primarily handles idempotency checks and persistence for the Kafka ingestion pipeline.
 */
@Repository
public interface ExternalContractRepository extends JpaRepository<ExternalContract, UUID> {

    /**
     * Checks if a contract with the specified external identifier already exists in the system.
     * Used to prevent duplicate processing during high-volume Kafka data streams.
     *
     * @param externalId The unique identifier provided by the external data source (e.g., Hlídač Státu ID).
     * @return true if the contract is already registered in the database.
     */
    boolean existsByExternalId(String externalId);

}
