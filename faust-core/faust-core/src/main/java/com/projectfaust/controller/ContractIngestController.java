package com.projectfaust.controller;

import com.projectfaust.service.ContractIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Controller responsible for orchestrating data ingestion from external contract registries.
 * Acts as the entry point for triggering data synchronization tasks.
 */
@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class ContractIngestController {

    private final ContractIngestService ingestService;

    /**
     * Triggers a contract ingestion process for a specific organization based on its Identification Number (IČO).
     * The process fetches data from the source and streams it into the Kafka processing pipeline.
     *
     * @param ico The unique Identification Number of the entity (e.g., National Security Authority: 68403561).
     * @return A status message confirming the initiation of the ingestion sequence.
     */
    @GetMapping("/contracts/{ico}")
    public String triggerIngest(@PathVariable String ico) {
        ingestService.fetchContractsForIco(ico);
        return "Contract ingestion sequence for ID " + ico + " has been dispatched to Kafka.";
    }
}
