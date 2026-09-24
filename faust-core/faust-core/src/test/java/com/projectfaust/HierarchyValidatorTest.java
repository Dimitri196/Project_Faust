package com.projectfaust;

import static org.junit.jupiter.api.Assertions.*;

import com.projectfaust.institution.Institution;
import com.projectfaust.shared.validator.HierarchyValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class HierarchyValidatorTest {

    private HierarchyValidator validator;
    private Institution alpha;
    private Institution beta;
    private Institution gamma;

    @BeforeEach
    void setUp() {
        validator = new HierarchyValidator();

        // Initialize sample institutions
        alpha = Institution.builder()
                .name("Alpha Corp")
                .externalId(UUID.randomUUID())
                .build();

        beta = Institution.builder()
                .name("Beta Dept")
                .externalId(UUID.randomUUID())
                .build();

        gamma = Institution.builder()
                .name("Gamma Unit")
                .externalId(UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("Should allow valid hierarchy (Alpha -> Beta -> Gamma)")
    void testValidHierarchy() {
        // Alpha is root, Beta is child of Alpha
        beta.setParent(alpha);

        // We want to make Gamma a child of Beta
        assertDoesNotThrow(() -> validator.verifyNoCircularReference(gamma, beta));
    }

    @Test
    @DisplayName("Should prevent an institution from being its own parent")
    void testSelfParenting() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                validator.verifyNoCircularReference(alpha, alpha)
        );

        assertEquals("An institution cannot be its own parent.", exception.getMessage());
    }

    @Test
    @DisplayName("Should prevent direct circular reference (A -> B -> A)")
    void testDirectCycle() {
        // Existing: Alpha is parent of Beta
        beta.setParent(alpha);

        // Attempt: Make Beta the parent of Alpha
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                validator.verifyNoCircularReference(alpha, beta)
        );

        assertTrue(exception.getMessage().contains("Circular reference detected"));
    }

    @Test
    @DisplayName("Should prevent deep circular reference (A -> B -> C -> A)")
    void testDeepCycle() {
        // Hierarchy: Alpha -> Beta -> Gamma
        beta.setParent(alpha);
        gamma.setParent(beta);

        // Attempt: Make Gamma the parent of Alpha (completing the loop)
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                validator.verifyNoCircularReference(alpha, gamma)
        );

        assertTrue(exception.getMessage().contains("is already a descendant of Alpha"));
    }

    @Test
    @DisplayName("Should handle nulls gracefully")
    void testNullHandling() {
        // If a node is a root (parent is null), it should always pass
        assertDoesNotThrow(() -> validator.verifyNoCircularReference(alpha, null));
    }
}