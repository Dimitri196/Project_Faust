package com.projectfaust.specification;

import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;
import com.projectfaust.entity.filters.LocationFilter;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;


/**
 * Robustní specifikace pro Project Faust.
 * Zajišťuje dynamické filtrování lokací na základě geografických,
 * hierarchických a bezpečnostních parametrů.
 */
public class LocationSpecifications {

    /**
     * Základní bezpečnostní pravidlo: vracíme pouze aktivní uzly.
     */
    public static Specification<Location> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Full-textové vyhledávání v názvu lokace (case-insensitive).
     */
    public static Specification<Location> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Filtrace podle typu lokace (COUNTRY, CITY, FACILITY atd.).
     */
    public static Specification<Location> hasType(LocationType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    /**
     * Vyhledá přímé potomky daného rodiče pomocí UUID.
     */
    public static Specification<Location> hasParent(UUID parentExternalId) {
        return (root, query, cb) -> {
            if (parentExternalId == null) return null;
            return cb.equal(root.get("parent").get("externalId"), parentExternalId);
        };
    }

    /**
     * Bezpečnostní filtr (Project Faust): vrátí pouze lokace,
     * na které má uživatel dostatečnou prověrku.
     */
    public static Specification<Location> securityScope(ClearanceLevel maxLevel) {
        return (root, query, cb) -> maxLevel == null
                ? null
                : cb.lessThanOrEqualTo(root.get("clearanceLevel"), maxLevel);
    }

    /**
     * Geofencing: filtrace lokací uvnitř mapového výřezu.
     */
    public static Specification<Location> insideBounds(Double north, Double south, Double east, Double west) {
        return (root, query, cb) -> {
            if (north == null || south == null || east == null || west == null) return null;
            return cb.and(
                    cb.between(root.get("latitude"), south, north),
                    cb.between(root.get("longitude"), west, east)
            );
        };
    }

    /**
     * Vyfiltruje pouze kořenové elementy (kontinenty).
     */
    public static Specification<Location> isRoot() {
        return (root, query, cb) -> cb.isNull(root.get("parent"));
    }

    /**
     * Sestaví finální komplexní Query na základě LocationFilteru.
     * Implementuje inteligentní hierarchický drill-down.
     */
    public static Specification<Location> build(LocationFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Vždy vynutit aktivní záznamy
            predicates.add(cb.isTrue(root.get("active")));

            if (filter != null) {
                // 2. Full-text vyhledávání
                if (filter.getQuery() != null && !filter.getQuery().isBlank()) {
                    String pattern = "%" + filter.getQuery().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(root.get("name")), pattern));
                }

                // 3. Typ lokace
                if (filter.getType() != null) {
                    predicates.add(cb.equal(root.get("type"), filter.getType()));
                }

                // 4. Hierarchie (Drill-down logika)
                if (filter.getParentId() != null) {
                    // Pokud je zadán rodič, hledáme v něm (vrtání do hloubky)
                    predicates.add(cb.equal(root.get("parent").get("externalId"), filter.getParentId()));
                } else if (Boolean.TRUE.equals(filter.getRootOnly())) {
                    // Pokud jsme na začátku a není search, chceme jen kořeny
                    predicates.add(cb.isNull(root.get("parent")));
                }

                // 5. Geografické hranice
                if (filter.getNorth() != null && filter.getSouth() != null &&
                        filter.getEast() != null && filter.getWest() != null) {
                    predicates.add(cb.between(root.get("latitude"), filter.getSouth(), filter.getNorth()));
                    predicates.add(cb.between(root.get("longitude"), filter.getWest(), filter.getEast()));
                }

                // 6. Bezpečnostní clearance (Project Faust)
                if (filter.getMaxClearance() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("clearanceLevel"), filter.getMaxClearance()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}