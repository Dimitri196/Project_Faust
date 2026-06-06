package com.projectfaust.service;

import com.projectfaust.dto.request.ContactOperationsRequest;
import com.projectfaust.dto.response.ContactOperationsResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.PersonContact;
import com.projectfaust.entity.enums.ContactType;
import com.projectfaust.mapper.PersonContactMapper;
import com.projectfaust.repository.PersonContactRepository;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonContactService {

    private final PersonContactRepository contactRepository;
    private final PersonRepository personRepository;
    private final PersonContactMapper mapper;

    @Transactional
    public ContactOperationsResponse addContact(ContactOperationsRequest request) {
        log.info("Attaching new intelligence vector to subject: {}", request.personPublicId());

        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Anchor identity master file not found"));

        String normalizedValue = normalizeContactValue(request.contactType(), request.contactValueRaw());

        PersonContact contact = mapper.toEntity(request);
        contact.setPerson(person);
        contact.setContactValueNormalized(normalizedValue);

        // Vyčištění hardwarového řetězce (IMEI) od mezer, pokud existuje
        if (contact.getImei() != null) {
            contact.setImei(contact.getImei().replaceAll("\\s+", ""));
        }

        return mapper.toResponse(contactRepository.save(contact));
    }

    @Transactional
    public ContactOperationsResponse updateContact(UUID contactPublicId, ContactOperationsRequest request) {
        log.info("Modifying technical telemetry footprint vector: {}", contactPublicId);

        PersonContact contact = contactRepository.findByExternalId(contactPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Requested contact vector not found"));

        mapper.updateEntityFromRequest(request, contact);

        String normalizedValue = normalizeContactValue(request.contactType(), request.contactValueRaw());
        contact.setContactValueNormalized(normalizedValue);

        if (contact.getImei() != null) {
            contact.setImei(contact.getImei().replaceAll("\\s+", ""));
        }

        return mapper.toResponse(contactRepository.save(contact));
    }

    @Transactional(readOnly = true)
    public List<ContactOperationsResponse> getContactsByPerson(UUID personPublicId) {
        return mapper.toResponseList(contactRepository.findAllByPersonExternalId(personPublicId));
    }

    @Transactional
    public void revokeContact(UUID contactPublicId) {
        log.warn("Purging telemetry trace entity from active deployment: {}", contactPublicId);
        PersonContact contact = contactRepository.findByExternalId(contactPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Target vector not found"));

        contactRepository.delete(contact);
    }

    @Transactional(readOnly = true)
    public List<ContactOperationsResponse> checkSharedInfrastructure(UUID contactPublicId) {
        PersonContact contact = contactRepository.findByExternalId(contactPublicId)
                .orElseThrow(() -> new EntityNotFoundException("Target vector not found"));

        List<PersonContact> leaks = contactRepository.findSharedInfrastructureLeaks(
                contact.getContactValueNormalized(),
                contact.getPerson().getExternalId()
        );
        return mapper.toResponseList(leaks);
    }

    private String normalizeContactValue(ContactType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Raw vector trace cannot be empty");
        }
        String clean = rawValue.trim();
        if (type == ContactType.EMAIL) return clean.toLowerCase();
        if (type == ContactType.CELLULAR_GSM) return clean.replaceAll("[^0-9+]", "");
        return clean;
    }
}