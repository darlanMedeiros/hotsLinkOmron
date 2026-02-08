package org.ctrl.db.model;

import java.time.LocalDateTime;

public class TagValue {

    private final String tagName;
    private final String memoryName;
    private final String deviceMnemonic;
    private final Integer value;
    private final LocalDateTime updatedAt;

    public TagValue(String tagName, String memoryName, String deviceMnemonic, Integer value, LocalDateTime updatedAt) {
        if (tagName == null || tagName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        this.tagName = tagName;
        this.memoryName = memoryName;
        this.deviceMnemonic = deviceMnemonic;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    public String getTagName() {
        return tagName;
    }

    public String getMemoryName() {
        return memoryName;
    }

    public String getDeviceMnemonic() {
        return deviceMnemonic;
    }

    public Integer getValue() {
        return value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
