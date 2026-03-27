package com.projectfaust.specification;

import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;
import com.projectfaust.entity.filters.LocationFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class LocationSpecifications {

    public static Specification<Location> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Location> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Location> hasType(String type) {
        return (root, query, cb) -> {
            if (type == null || type.isBlank() || type.equals("ALL_TYPES")) return null;
            try {
                LocationType enumType = LocationType.valueOf(type.toUpperCase());
                return cb.equal(root.get("type"), enumType);
            } catch (IllegalArgumentException e) {
                return null;
            }
        };
    }

    public static Specification<Location> hasParent(UUID parentExternalId) {
        return (root, query, cb) -> {
            if (parentExternalId == null) return null;
            return cb.equal(root.get("parent").get("externalId"), parentExternalId);
        };
    }

    public static Specification<Location> securityScope(ClearanceLevel maxLevel) {
        return (root, query, cb) -> maxLevel == null
                ? null
                : cb.lessThanOrEqualTo(root.get("clearanceLevel"), maxLevel);
    }

    public static Specification<Location> insideBounds(Double north, Double south, Double east, Double west) {
        return (root, query, cb) -> {
            if (north == null || south == null || east == null || west == null) return null;
            return cb.and(
                    cb.between(root.get("latitude"), south, north),
                    cb.between(root.get("longitude"), west, east)
            );
        };
    }

    public static Specification<Location> isRoot() {
        return (root, query, cb) -> cb.isNull(root.get("parent"));
    }

    /**
     * Sestaví finální query na základě LocationFilteru.
     */
    public static Specification<Location> build(LocationFilter filter) {
        Specification<Location> spec = Specification.where(activeOnly());

        if (filter == null) return spec;

        // Základní filtry
        spec = spec.and(nameContains(filter.getQuery()));
        spec = spec.and(hasType(filter.getType() != null ? filter.getType().name() : null));

        // Hierarchie
        if (Boolean.TRUE.equals(filter.getRootOnly())) {
            spec = spec.and(isRoot());
        } else {
            spec = spec.and(hasParent(filter.getParentId()));
        }

        // Mapové souřadnice
        spec = spec.and(insideBounds(filter.getNorth(), filter.getSouth(), filter.getEast(), filter.getWest()));

        // Projekt Faust: Bezpečnostní ořezání
        spec = spec.and(securityScope(filter.getMaxClearance()));

        return spec;
    }
}
