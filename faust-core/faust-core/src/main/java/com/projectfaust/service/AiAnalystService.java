package com.projectfaust.service;

import com.projectfaust.entity.IntelligenceReport;
import com.projectfaust.entity.Person;
import com.projectfaust.repository.IntelligenceReportRepository;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiAnalystService {

    private final PersonRepository personRepository;
    private final IntelligenceReportRepository reportRepository;
    private final AiService aiService;
    private final IntelligenceService intelligenceService;

    @Transactional
    public String getAiIntelligenceBrief(UUID externalId) {

        Person person = personRepository.findFullProfileByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("Subjekt nenalezen v registru."));

        Optional<IntelligenceReport> existingReport = reportRepository.findFirstByPersonOrderByGeneratedAtDesc(person);
        if (existingReport.isPresent() &&
                existingReport.get().getGeneratedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            return existingReport.get().getAnalysisResult();
        }

        StringBuilder context = new StringBuilder();

        String prompt = String.format("""
Jsi analytický modul Projektu FAUST. Vypracuj bezpečnostní profil PŘÍSNĚ na základě dodaných dat.

SUBJEKT K ANALÝZE:
Jméno: %s %s
Narození: %s
Pozice: %s
Prověření: %s

INSTRUKCE:
- POUŽIJ POUZE data ze zaslaného kontextu. 
- Pokud v datech něco není (např. plat asistentky), NEVYMÝŠLEJ SI TO.
- Ignoruj své tréninkové placeholdery (jako Petr Novák).
- Pokud detekuješ anomálii v kariéře (např. skok z ODA do čela NBÚ), označ to [ANOMALY_DETECTED].

STRUKTURA HLÁŠENÍ:
- IDENT_DATA: Fakta o subjektu.
- RISK_ASSESSMENT: Analýza vazeb a rizik.
- INFLUENCE_TRACE: Rozsah vlivu.
""", person.getFirstName(), person.getLastName(), person.getBirthDate(), person.getBiography(), person.getClearanceLevel());

        String rawAnalysis = aiService.askGpt(prompt + "\n\n KONTEXT Z DATABÁZE: \n" + context.toString());

        String finalAnalysis = rawAnalysis
                .replaceAll("(?i)Dušan", "**DUŠAN**")
                .replaceAll("(?i)Navrátil", "**NAVRÁTIL**")
                .replaceAll("(?i)střet zájmů", "[CRITICAL_CONFLICT_OF_INTEREST_DETECTED]")
                // ... další flagy ...
                .replaceAll("(?i)nepotismus", "[NEPOTISM_FLAG_ALPHA]");

        IntelligenceReport report = IntelligenceReport.builder()
                .person(person)
                .externalId(UUID.randomUUID())
                .analysisResult(finalAnalysis)
                .generatedAt(LocalDateTime.now())
                .modelVersion("gemini-3-flash")
                .build();

        reportRepository.save(report);

        return finalAnalysis;
    }
}
