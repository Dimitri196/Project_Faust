package com.projectfaust.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link BankAccount} entities.
 *
 * @author Dimitri / Project Faust
 */
public interface BankAccountRepository
        extends JpaRepository<BankAccount, Long>,
        JpaSpecificationExecutor<BankAccount> {

    Optional<BankAccount> findByExternalId(UUID externalId);

    Optional<BankAccount> findByIban(String iban);

    List<BankAccount> findByMonitoredTrue();

    List<BankAccount> findByCurrencyIgnoreCase(String currency);

    List<BankAccount> findByBankNameContainingIgnoreCase(String bankName);

    boolean existsByExternalId(UUID externalId);

    /**
     * All accounts linked (via person relations) to a specific person.
     */
    @Query("""
            SELECT DISTINCT ba FROM BankAccount ba
            JOIN ba.accountHolders par
            WHERE par.person.externalId = :personExternalId
            ORDER BY ba.bankName ASC
            """)
    List<BankAccount> findByPersonExternalId(@Param("personExternalId") UUID personExternalId);

    /**
     * All accounts linked (via institution relations) to a specific institution.
     */
    @Query("""
            SELECT DISTINCT ba FROM BankAccount ba
            JOIN ba.institutionalOwners iar
            WHERE iar.institution.externalId = :institutionExternalId
            ORDER BY ba.bankName ASC
            """)
    List<BankAccount> findByInstitutionExternalId(@Param("institutionExternalId") UUID institutionExternalId);

    /**
     * Accounts shared by two persons — FININT co-holder network detection.
     */
    @Query("""
            SELECT DISTINCT ba FROM BankAccount ba
            JOIN ba.accountHolders r1
            JOIN ba.accountHolders r2
            WHERE r1.person.externalId = :personA
              AND r2.person.externalId = :personB
            ORDER BY ba.iban ASC
            """)
    List<BankAccount> findSharedByTwoPersons(
            @Param("personA") UUID personA,
            @Param("personB") UUID personB);
}
