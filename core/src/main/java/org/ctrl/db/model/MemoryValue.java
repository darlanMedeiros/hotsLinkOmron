package org.ctrl.db.model;

import java.time.LocalDateTime;

public class MemoryValue {

    private final String name;
    private final int value;
    private final LocalDateTime updatedAt;

    public MemoryValue(String name, int value, LocalDateTime updatedAt) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        this.name = name;
        this.value = value;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
