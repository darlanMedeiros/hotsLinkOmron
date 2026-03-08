package org.ctrl.db.api.model;

public class TagCrud {

    private final Integer id;
    private final String name;
    private final Integer deviceId;
    private final Integer memoryId;
    private final Boolean persistHistory;

    public TagCrud(Integer id, String name, Integer deviceId, Integer memoryId, Boolean persistHistory) {
        this.id = id;
        this.name = name;
        this.deviceId = deviceId;
        this.memoryId = memoryId;
        this.persistHistory = persistHistory;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public Integer getMemoryId() {
        return memoryId;
    }

    public Boolean getPersistHistory() {
        return persistHistory;
    }
}
