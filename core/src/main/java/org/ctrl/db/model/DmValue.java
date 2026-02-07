package org.ctrl.db.model;

import java.time.Instant;

public class DmValue {

    private final int address;
    private final int value;
    private final Instant updatedAt;

    public DmValue(int address, int value, Instant updatedAt) {
        if (address < 0 || address > 1000) {
            throw new IllegalArgumentException("Address must be between 0 and 1000");
        }
        this.address = address;
        this.value = value;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public int getAddress() {
        return address;
    }

    public int getValue() {
        return value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
