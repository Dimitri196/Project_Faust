package com.projectfaust.validator;

import org.springframework.stereotype.Component;

@Component
public class HierarchyValidator {

    public <T extends Hierarchical<T>> void verifyNoCircularReference(T entity, T proposedParent) {
        if (entity == null || proposedParent == null) {
            return;
        }

        if (entity.getExternalId().equals(proposedParent.getExternalId())) {
            throw new IllegalStateException(
                    String.format("'%s' cannot be its own parent/supervisor.", entity.getName())
            );
        }

        T current = proposedParent;
        while (current != null) {
            if (current.getExternalId().equals(entity.getExternalId())) {
                throw new IllegalStateException(
                        String.format("Circular reference detected: '%s' is already a descendant of '%s'.",
                                proposedParent.getName(), entity.getName())
                );
            }
            current = current.getParent();
        }
    }
}
