package com.projectfaust.ingest.hlidacstatu;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for triggering external contract data fetches.
 *
 * <p>Triggers an asynchronous fetch from Hlidac Statu — results are
 * dispatched to Kafka and processed by {@link ContractConsumerService}.
 * This endpoint returns immediately; ingestion happens in the background.</p>
 *
 * <p>Base path: {@code /api/v1/ingest/fetch}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/ingest/fetch")
@RequiredArgsConstructor
@Tag(name = "Contract Ingestion", description = "Trigger external contract data fetches")
public class ExternalContractFetchController {

    private final ExternalContractFetchService fetchService;

    /**
     * Triggers a fetch of all contracts associated with a given Czech company
     * registration number (IČO) from Hlidac Statu.
     *
     * @param ico the 8-digit Czech company registration number.
     * @return confirmation that the fetch was dispatched.
     */
    @GetMapping("/hlidac-statu/{ico}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Fetch contracts by ICO from Hlidac Statu",
            description = "Triggers an async fetch from Hlidac Statu for the given company registration number. Results are dispatched to Kafka for processing.")
    public ResponseEntity<String> triggerFetch(@PathVariable String ico) {
        fetchService.fetchContractsForIco(ico);
        return ResponseEntity.ok("Contract ingestion for ICO " + ico + " dispatched to Kafka.");
    }
}