package com.projectfaust.specification;

import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.LocationType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class LocationSpecifications {

    public static Specification<Location> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isEmpty())
                ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Location> hasType(String type) {
        return (root, query, cb) -> {
            if (type == null || type.isEmpty() || type.equals("ALL_SECTOR_TYPES")) return null;
            try {
                // Converts "city" or "CITY" to the Enum safely
                LocationType enumType = LocationType.valueOf(type.toUpperCase());
                return cb.equal(root.get("type"), enumType);
            } catch (IllegalArgumentException e) {
                return null; // Or handle as an error
            }
        };
    }

    public static Specification<Location> hasParent(UUID parentExternalId) {
        return (root, query, cb) -> {
            if (parentExternalId == null) return null;
            return cb.equal(root.get("parent").get("externalId"), parentExternalId);
        };
    }
}