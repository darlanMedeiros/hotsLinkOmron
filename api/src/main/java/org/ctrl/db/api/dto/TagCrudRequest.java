package org.ctrl.db.api.dto;

public class TagCrudRequest {

    private String name;
    private Integer deviceId;
    private Integer memoryId;
    private Boolean persistHistory;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Integer memoryId) {
        this.memoryId = memoryId;
    }

    public Boolean getPersistHistory() {
        return persistHistory;
    }

    public void setPersistHistory(Boolean persistHistory) {
        this.persistHistory = persistHistory;
    }
}
