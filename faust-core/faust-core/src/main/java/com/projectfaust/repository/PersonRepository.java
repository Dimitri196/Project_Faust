package com.projectfaust.repository;

import com.projectfaust.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByExternalId(UUID externalId);

    List<Person> findAllByEmailIn(Collection<String> emails);

    boolean existsByFirstNameAndLastName(String firstName, String lastName);

    boolean existsByEmail(String email);

    @Query(value = "SELECT p FROM Person p WHERE p.fullNameSearchNormalized LIKE " +
            "LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByFullName(@Param("query") String query);
}
