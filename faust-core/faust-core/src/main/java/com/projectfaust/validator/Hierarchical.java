package com.projectfaust.validator;

import java.util.UUID;

public interface Hierarchical<T> {
    UUID getExternalId();
    T getParent();
    String getName(); // For descriptive error messages
}
