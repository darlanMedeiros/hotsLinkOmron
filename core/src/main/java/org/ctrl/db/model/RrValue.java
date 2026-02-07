package org.ctrl.db.model;

import java.time.Instant;

public class RrValue {

    private final int address;
    private final int bit;
    private final boolean value;
    private final Instant updatedAt;

    public RrValue(int address, int bit, boolean value, Instant updatedAt) {
        if (address < 0) {
            throw new IllegalArgumentException("Address must be >= 0");
        }
        if (bit < 0 || bit > 15) {
            throw new IllegalArgumentException("Bit must be between 0 and 15");
        }
        this.address = address;
        this.bit = bit;
        this.value = value;
        this.updatedAt = updatedAt == null ? Instant.now() : updatedAt;
    }

    public int getAddress() {
        return address;
    }

    public int getBit() {
        return bit;
    }

    public boolean isValue() {
        return value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
