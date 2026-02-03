package com.projectfaust.validator;

import org.springframework.stereotype.Component;

@Component
public class HierarchyValidator {

    /**
     * Validates that setting 'proposedParent' as the parent of 'entity'
     * does not create a cycle for any hierarchical type.
     */
    public <T extends Hierarchical<T>> void verifyNoCircularReference(T entity, T proposedParent) {
        if (entity == null || proposedParent == null) {
            return;
        }

        // 1. Self-reference check
        if (entity.getExternalId().equals(proposedParent.getExternalId())) {
            throw new IllegalStateException(
                    String.format("'%s' cannot be its own parent/supervisor.", entity.getName())
            );
        }

        // 2. Upward traversal check
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
