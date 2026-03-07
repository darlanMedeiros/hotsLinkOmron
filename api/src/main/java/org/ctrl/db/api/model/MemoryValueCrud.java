package org.ctrl.db.api.model;

import java.time.LocalDateTime;

public class MemoryValueCrud {

    private final Integer id;
    private final Integer memoryId;
    private final Integer value;
    private final Boolean status;
    private final LocalDateTime updatedAt;

    public MemoryValueCrud(Integer id, Integer memoryId, Integer value, Boolean status, LocalDateTime updatedAt) {
        this.id = id;
        this.memoryId = memoryId;
        this.value = value;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public Integer getValue() {
        return value;
    }

    public Boolean getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
