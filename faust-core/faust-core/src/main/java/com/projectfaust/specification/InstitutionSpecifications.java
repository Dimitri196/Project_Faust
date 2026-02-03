package com.projectfaust.specification;

import com.projectfaust.entity.Institution;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import org.springframework.data.jpa.domain.Specification;

public class InstitutionSpecifications {

    public static Specification<Institution> hasType(InstitutionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Institution> hasLevel(HierarchicalLevel level) {
        return (root, query, cb) -> level == null ? null : cb.equal(root.get("level"), level);
    }

    public static Specification<Institution> nameContains(String name) {
        return (root, query, cb) -> name == null ? null :
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Institution> hasCountry(String countryCode) {
        return (root, query, cb) -> countryCode == null ? null :
                cb.equal(root.get("countryCode"), countryCode.toUpperCase());
    }

    public static Specification<Institution> isStateOwned(Boolean isStateOwned) {
        return (root, query, cb) -> isStateOwned == null ? null :
                cb.equal(root.get("isStateOwned"), isStateOwned);
    }
}
