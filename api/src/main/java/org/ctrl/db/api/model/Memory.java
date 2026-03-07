package org.ctrl.db.api.model;

public class Memory {

    private final Integer id;
    private final Integer deviceId;
    private final String name;

    public Memory(Integer id, Integer deviceId, String name) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public String getName() {
        return name;
    }
}
