package org.ctrl.db.model;

public class Tag {

    private final Integer id;
    private final String name;
    private final int deviceId;
    private final int memoryId;

    public Tag(Integer id, String name, int deviceId, int memoryId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag name is required");
        }
        this.id = id;
        this.name = name;
        this.deviceId = deviceId;
        this.memoryId = memoryId;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getMemoryId() {
        return memoryId;
    }
}
