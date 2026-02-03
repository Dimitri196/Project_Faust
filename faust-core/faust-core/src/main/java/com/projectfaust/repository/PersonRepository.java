package com.projectfaust.repository;

import com.projectfaust.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByExternalId(UUID externalId);

    // Useful for search functionality in the frontend
    List<Person> findByLastNameContainingIgnoreCase(String lastName);

    // Check if email is already taken
    boolean existsByEmail(String email);

    // Vyhledá osoby, kde jméno NEBO příjmení obsahuje daný řetězec
    List<Person> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    @Query(value = "SELECT p FROM Person p WHERE p.fullNameSearchNormalized LIKE " +
            "LOWER(CONCAT('%', f_unaccent(:query), '%'))")
    List<Person> searchByFullName(@Param("query") String query);
}