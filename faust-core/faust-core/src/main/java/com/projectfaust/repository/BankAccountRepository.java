package com.projectfaust.repository;

import com.projectfaust.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    /**
     * Vyhledá bankovní účet podle jeho externího UUID trasovacího identifikátoru.
     */
    Optional<BankAccount> findByExternalId(UUID externalId);

    /**
     * Vyhledá bankovní účet podle IBANu.
     * Klíčové pro detekci duplicit před zápisem nové finanční telemetrie.
     */
    Optional<BankAccount> findByIban(String iban);

    /**
     * Vrátí seznam všech účtů, které jsou označené jako monitorované finančním zpravodajstvím.
     */
    List<BankAccount> findByIsMonitoredTrue();

    /**
     * Načte kompletní finanční uzel včetně všech historických vazeb na osoby a instituce.
     * Správně fetchuje relace a následně i koncové entity (Person, Institution) v jednom dotazu.
     */
    @Query("SELECT b FROM BankAccount b " +
            "LEFT JOIN FETCH b.accountHolders ph " +
            "LEFT JOIN FETCH ph.person " +
            "LEFT JOIN FETCH b.institutionalOwners io " +
            "LEFT JOIN FETCH io.institution " +
            "WHERE b.externalId = :externalId")
    Optional<BankAccount> findFullNodeProfileByExternalId(@Param("externalId") UUID externalId);

    /**
     * Vyhledá účty podle názvu finanční instituce (case-insensitive).
     */
    List<BankAccount> findByBankNameContainingIgnoreCase(String bankName);
}