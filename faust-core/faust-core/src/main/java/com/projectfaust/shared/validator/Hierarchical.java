package com.projectfaust.shared.validator;

import java.util.UUID;

/**
 * Marker interface for entities that participate in a parent-child hierarchy
 * within Project Faust.
 *
 * <p>Any entity that can be organised into a tree structure — {@code Location},
 * {@code Institution}, {@code Occupation} — implements this interface to gain
 * compatibility with the generic {@link HierarchyValidator}. This avoids
 * duplicating circular-reference detection logic per domain.</p>
 *
 * <p><b>Contract:</b></p>
 * <ul>
 *   <li>{@link #getExternalId()} must return a stable, non-null UUID for every
 *       persisted instance — it is used as the identity key during cycle detection.</li>
 *   <li>{@link #getParent()} must return {@code null} for root nodes.</li>
 *   <li>{@link #getName()} is used solely for human-readable error messages.</li>
 * </ul>
 *
 * <p><b>Important:</b> implementations using JPA lazy loading must ensure that
 * {@link #getParent()} is called within an active Hibernate session. The
 * {@link HierarchyValidator} walks the ancestor chain via this method, which
 * triggers a lazy load per step if the parent collection is not eagerly fetched
 * or pre-loaded by a CTE query.</p>
 *
 * @param <T> the concrete entity type implementing this interface.
 * @author Dimitri / Project Faust
 */
public interface Hierarchical<T> {

    /**
     * Returns the public UUID of this node.
     *
     * <p>Used as the identity key in cycle detection — two nodes are considered
     * the same if and only if their {@code externalId} values are equal.</p>
     *
     * @return the non-null public UUID of this node.
     */
    UUID getExternalId();

    /**
     * Returns the direct parent of this node, or {@code null} if this is a root node.
     *
     * @return the parent node, or {@code null}.
     */
    T getParent();

    /**
     * Returns the human-readable name of this node.
     *
     * <p>Used exclusively for constructing diagnostic error messages in
     * {@link HierarchyValidator}.</p>
     *
     * @return the name of this node, never {@code null}.
     */
    String getName();
}
