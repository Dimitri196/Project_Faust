package com.projectfaust.specification;

import com.projectfaust.entity.Institution;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class InstitutionSpecifications {

    public static Specification<Institution> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    public static Specification<Institution> hasSecurityLevel(ClearanceLevel clearanceLevel) {
        return (root, query, cb) -> clearanceLevel == null ? null :
                cb.equal(root.get("securityLevel"), clearanceLevel);
    }

    public static Specification<Institution> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) ? null :
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Institution> hasCountry(String countryCode) {
        return (root, query, cb) -> {
            if (countryCode == null || countryCode.isBlank()) return null;
            return cb.equal(root.get("location").get("isoCode"), countryCode.toUpperCase());
        };
    }

    public static Specification<Institution> isStateOwned(Boolean isStateOwned) {
        return (root, query, cb) -> isStateOwned == null ? null :
                cb.equal(root.get("isStateOwned"), isStateOwned);
    }

    public static Specification<Institution> hasType(InstitutionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Institution> hasLevel(HierarchicalLevel level) {
        return (root, query, cb) -> level == null ? null : cb.equal(root.get("level"), level);
    }

    public static Specification<Institution> hasLocation(UUID locationId) {
        return (root, query, cb) -> {
            if (locationId == null) return null;
            return cb.equal(root.get("location").get("externalId"), locationId);
        };
    }

    public static Specification<Institution> hasParent(UUID parentId) {
        return (root, query, cb) -> {
            if (parentId == null) return null;
            return cb.equal(root.get("parent").get("externalId"), parentId);
        };
    }
}
