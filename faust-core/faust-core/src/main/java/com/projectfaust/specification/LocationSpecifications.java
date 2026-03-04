package com.projectfaust.specification;

import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.LocationType;
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
}
