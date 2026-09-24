package com.projectfaust.shared.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Domain-agnostic validator for detecting circular references in parent-child hierarchies.
 *
 * <p>Used across all hierarchical domains in Project Faust — {@code Location},
 * {@code Institution}, and {@code Occupation} — to enforce the invariant that
 * no node may be its own ancestor.</p>
 *
 * <p>The validator operates on any type that implements {@link Hierarchical},
 * requiring no coupling to a specific entity class.</p>
 *
 * <p><b>Algorithm:</b> two checks are performed in order:</p>
 * <ol>
 *   <li><b>Self-reference check</b> — fast path; rejects the case where a node
 *       is assigned as its own parent before walking the chain.</li>
 *   <li><b>Ancestor walk</b> — traverses the proposed parent's ancestor chain
 *       upward. If the entity being validated appears anywhere in that chain,
 *       assigning it as the parent would close a cycle.</li>
 * </ol>
 *
 * <p><b>Performance note:</b> the ancestor walk calls {@link Hierarchical#getParent()}
 * at each step. For JPA entities with lazy-loaded parents this triggers one
 * database round trip per ancestor level. Callers should ensure the hierarchy
 * is pre-loaded (e.g., via a recursive CTE query) before invoking this validator
 * when deep hierarchies are expected.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Component
public class HierarchyValidator {

    /**
     * Verifies that assigning {@code proposedParent} as the parent of {@code entity}
     * would not introduce a circular reference.
     *
     * <p>Two conditions are checked:</p>
     * <ul>
     *   <li>The entity is not being assigned as its own parent.</li>
     *   <li>The entity does not already appear as an ancestor of the proposed parent.</li>
     * </ul>
     *
     * <p>If either parameter is {@code null} (e.g., when assigning a root node),
     * the method returns immediately without throwing.</p>
     *
     * @param <T>            the concrete hierarchical entity type.
     * @param entity         the node that is about to receive a new parent.
     * @param proposedParent the node that is being proposed as the new parent.
     * @throws IllegalStateException if a self-reference or circular reference is detected.
     */
    public <T extends Hierarchical<T>> void verifyNoCircularReference(T entity, T proposedParent) {
        if (entity == null || proposedParent == null) {
            return;
        }

        // Fast path: self-reference check
        if (entity.getExternalId().equals(proposedParent.getExternalId())) {
            throw new IllegalStateException(String.format(
                    "HIERARCHY_VIOLATION: '%s' cannot be its own parent.",
                    entity.getName()
            ));
        }

        // Walk the proposed parent's ancestor chain — if entity appears in it,
        // making it the parent would close a cycle.
        T current = proposedParent.getParent();
        while (current != null) {
            if (current.getExternalId().equals(entity.getExternalId())) {
                throw new IllegalStateException(String.format(
                        "HIERARCHY_VIOLATION: Circular reference detected — " +
                                "'%s' is already an ancestor of '%s'. " +
                                "Assigning it as parent would close a cycle.",
                        entity.getName(), proposedParent.getName()
                ));
            }
            current = current.getParent();
        }

        log.debug("FAUST_HIERARCHY: No circular reference detected for '{}' → '{}'.",
                entity.getName(), proposedParent.getName());
    }
}