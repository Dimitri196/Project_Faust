package com.projectfaust.repository;

import com.projectfaust.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends
        JpaRepository<Institution, Long>,
        JpaSpecificationExecutor<Institution> {

    @Query("SELECT i FROM Institution i LEFT JOIN FETCH i.children WHERE i.parent IS NULL")
    List<Institution> findAllRoots();

    Optional<Institution> findByExternalId(UUID externalId);
}
