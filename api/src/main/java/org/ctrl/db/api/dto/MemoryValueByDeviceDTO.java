package org.ctrl.db.api.dto;

import java.time.LocalDateTime;

public class MemoryValueByDeviceDTO {
    private int deviceId;
    private String plcMnemonic;
    private String tagName;
    private String memoryName;
    private int value;
    private LocalDateTime timestamp;

    public MemoryValueByDeviceDTO() {}

    public MemoryValueByDeviceDTO(int deviceId, String plcMnemonic, String tagName, String memoryName, int value, LocalDateTime timestamp) {
        this.deviceId = deviceId;
        this.plcMnemonic = plcMnemonic;
        this.tagName = tagName;
        this.memoryName = memoryName;
        this.value = value;
        this.timestamp = timestamp;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String getPlcMnemonic() {
        return plcMnemonic;
    }

    public void setPlcMnemonic(String plcMnemonic) {
        this.plcMnemonic = plcMnemonic;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getMemoryName() {
        return memoryName;
    }

    public void setMemoryName(String memoryName) {
        this.memoryName = memoryName;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
