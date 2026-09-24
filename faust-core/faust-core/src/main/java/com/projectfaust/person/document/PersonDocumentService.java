package com.projectfaust.person.document;

import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.person.document.dto.PersonDocumentRequest;
import com.projectfaust.person.document.dto.PersonDocumentResponse;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.DocumentSourceSystem;
import com.projectfaust.shared.enums.DocumentType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing intelligence document records on person entities.
 *
 * <p>Supports three ingestion paths:
 * <ol>
 *   <li>Manual entry via API</li>
 *   <li>ARES registry pull by IČO</li>
 *   <li>MRZ string parsing from document scan</li>
 * </ol>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonDocumentService {

    private final PersonDocumentRepository documentRepository;
    private final PersonRepository         personRepository;
    private final PersonDocumentMapper     mapper;
    private final AresRegistryService      aresService;
    private final MrzParserService         mrzParser;

    // Document scan storage path — configure in application.yaml
    private static final String SCAN_STORAGE_PATH = "data/document-scans/";

    // ── Manual entry ──────────────────────────────────────────────────────────

    @Transactional
    public PersonDocumentResponse addDocument(PersonDocumentRequest request) {
        Person person = resolvePerson(request.personPublicId());
        warnDuplicates(request.documentNumberRaw(), request.personPublicId());

        PersonDocument doc = mapper.toEntity(request, person);
        PersonDocument saved = documentRepository.save(doc);
        log.info("FAUST_DOC: Added {} document to person {}.",
                request.documentType(), request.personPublicId());
        return mapper.toResponse(saved);
    }

    // ── ARES registry pull ────────────────────────────────────────────────────

    /**
     * Fetches IČO data from ARES and creates a PersonDocument record.
     * Raw JSON response is stored for re-processing.
     *
     * @param ico            the 8-digit Czech company registration number.
     * @param personPublicId the person to link this document to.
     * @return created document record.
     */
    @Transactional
    public PersonDocumentResponse ingestFromAres(String ico, UUID personPublicId) {
        Person person = resolvePerson(personPublicId);

        AresRegistryService.AresResult ares = aresService.fetchByIco(ico)
                .orElseThrow(() -> new IllegalArgumentException(
                        "ARES returned no data for IČO: " + ico));

        PersonDocument doc = PersonDocument.builder()
                .person(person)
                .documentType(DocumentType.ICO)
                .documentNumberRaw(ares.ico())
                .issuingState("CZ")
                .issuingAuthority("ARES — Ministerstvo spravedlnosti ČR")
                .validFrom(ares.registeredAt())
                .validTo(ares.dissolvedAt())
                .active(ares.dissolvedAt() == null)
                .authentic(true)
                .sourceSystem(DocumentSourceSystem.ARES_CZ)
                .rawSourceData(ares.rawJson())
                .sourceReferenceId(ares.ico())
                .verificationStatus(VerificationStatus.OFFICIAL_REGISTRY)
                .confidenceScore(1.0)
                .clearanceLevel(ClearanceLevel.LEVEL_1_PUBLIC)
                .analyticalNote("Auto-ingested from ARES. Legal name: "
                        + ares.legalName()
                        + (ares.dic() != null ? " | DIČ: " + ares.dic() : ""))
                .build();

        // Also create DIČ record if available
        if (ares.dic() != null) {
            PersonDocument dicDoc = PersonDocument.builder()
                    .person(person)
                    .documentType(DocumentType.DIC)
                    .documentNumberRaw(ares.dic())
                    .issuingState("CZ")
                    .issuingAuthority("Finanční správa ČR")
                    .active(ares.dissolvedAt() == null)
                    .authentic(true)
                    .sourceSystem(DocumentSourceSystem.ARES_CZ)
                    .rawSourceData(ares.rawJson())
                    .verificationStatus(VerificationStatus.OFFICIAL_REGISTRY)
                    .confidenceScore(1.0)
                    .clearanceLevel(ClearanceLevel.LEVEL_1_PUBLIC)
                    .build();
            documentRepository.save(dicDoc);
            log.info("FAUST_DOC: Also created DIČ record {}.", ares.dic());
        }

        PersonDocument saved = documentRepository.save(doc);
        log.info("FAUST_DOC: Ingested IČO {} from ARES for person {}.", ico, personPublicId);
        return mapper.toResponse(saved);
    }

    // ── MRZ parsing ───────────────────────────────────────────────────────────

    /**
     * Parses an MRZ string and creates a PersonDocument record.
     *
     * @param mrz            raw MRZ string from document scan.
     * @param personPublicId the person to link this document to.
     * @param clearanceLevel minimum clearance for this record.
     * @return created document record.
     */
    @Transactional
    public PersonDocumentResponse ingestFromMrz(
            String mrz, UUID personPublicId, ClearanceLevel clearanceLevel) {

        Person person = resolvePerson(personPublicId);

        MrzParserService.MrzResult parsed = mrzParser.parse(mrz)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or unrecognized MRZ format."));

        // Map MRZ document type
        DocumentType docType = "P".equals(parsed.documentType())
                ? DocumentType.PASSPORT
                : DocumentType.NATIONAL_ID_CARD;

        PersonDocument doc = PersonDocument.builder()
                .person(person)
                .documentType(docType)
                .documentNumberRaw(parsed.documentNumber())
                .issuingState(parsed.issuingState())
                .validTo(parsed.expiryDate())
                .active(parsed.expiryDate() == null
                        || parsed.expiryDate().isAfter(java.time.LocalDate.now()))
                .authentic(true)
                .mrzLine(mrz)
                .sourceSystem(DocumentSourceSystem.MRZ_PARSER)
                .verificationStatus(VerificationStatus.TECHNICAL_INTERCEPT)
                .confidenceScore(0.9)
                .clearanceLevel(clearanceLevel)
                .analyticalNote("MRZ parsed. Name: " + parsed.fullName()
                        + " | DOB: " + parsed.dateOfBirth()
                        + " | Nationality: " + parsed.nationality()
                        + " | Format: " + parsed.format())
                .build();

        PersonDocument saved = documentRepository.save(doc);
        log.info("FAUST_DOC: Ingested {} from MRZ for person {}.",
                docType, personPublicId);
        return mapper.toResponse(saved);
    }

    // ── Document scan upload ──────────────────────────────────────────────────

    /**
     * Stores a document scan file and links it to an existing document record.
     *
     * @param documentExternalId the document to attach the scan to.
     * @param file               the uploaded scan file (PDF/JPG/PNG).
     * @return updated document record.
     */
    @Transactional
    public PersonDocumentResponse attachScan(UUID documentExternalId, MultipartFile file)
            throws IOException {

        PersonDocument doc = documentRepository.findByExternalId(documentExternalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document not found: " + documentExternalId));

        // Store file
        String filename = documentExternalId + "_" + file.getOriginalFilename();
        Path storagePath = Paths.get(SCAN_STORAGE_PATH);
        Files.createDirectories(storagePath);
        Path filePath = storagePath.resolve(filename);
        Files.write(filePath, file.getBytes());

        doc.setSourceImageUrl(filePath.toString());
        if (doc.getSourceSystem() == DocumentSourceSystem.MANUAL) {
            doc.setSourceSystem(DocumentSourceSystem.SCAN_OCR);
        }

        PersonDocument saved = documentRepository.save(doc);
        log.info("FAUST_DOC: Attached scan to document {}.", documentExternalId);
        return mapper.toResponse(saved);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PersonDocumentResponse> getByPerson(UUID personExternalId) {
        return documentRepository
                .findAllByPersonExternalIdOrderByValidToDesc(personExternalId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonDocumentResponse> getActiveByPerson(UUID personExternalId) {
        return documentRepository
                .findActiveByPersonExternalId(personExternalId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonDocumentResponse> findByDocumentNumber(
            String number, DocumentType type) {
        String normalized = number.toUpperCase().replaceAll("[\\s\\-]", "");
        return documentRepository
                .findByDocumentNumberAndType(normalized, type)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public void deactivate(UUID documentExternalId) {
        PersonDocument doc = documentRepository.findByExternalId(documentExternalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Document not found: " + documentExternalId));
        doc.setActive(false);
        documentRepository.save(doc);
        log.info("FAUST_DOC: Deactivated document {}.", documentExternalId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Person resolvePerson(UUID personPublicId) {
        return personRepository.findByExternalId(personPublicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Person not found: " + personPublicId));
    }

    private void warnDuplicates(String documentNumber, UUID personPublicId) {
        String normalized = documentNumber.toUpperCase().replaceAll("[\\s\\-]", "");
        List<PersonDocument> duplicates = documentRepository
                .findDuplicatesAcrossPersons(normalized, personPublicId);
        if (!duplicates.isEmpty()) {
            log.warn("FAUST_DOC: Document number {} already linked to {} other person(s).",
                    normalized, duplicates.size());
        }
    }
}
