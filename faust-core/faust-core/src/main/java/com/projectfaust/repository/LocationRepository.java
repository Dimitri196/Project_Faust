package com.projectfaust.repository;

import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, JpaSpecificationExecutor<Location> {

    Optional<Location> findByExternalId(UUID externalId);
    List<Location> findAllByParentIsNull();

    @Query("SELECT l FROM Location l WHERE l.parent.externalId = :parentExternalId")
    List<Location> findAllByParentExternalId(@Param("parentExternalId") UUID parentExternalId);

    @Query("SELECT l FROM Location l WHERE l.name = :name AND l.type = :type " +
            "AND (:parentExternalId IS NULL OR (l.parent IS NOT NULL AND l.parent.externalId = :parentExternalId))")
    Optional<Location> findByNameAndTypeAndParentExternalId(
            @Param("name") String name,
            @Param("type") LocationType type,
            @Param("parentExternalId") UUID parentExternalId);
}
