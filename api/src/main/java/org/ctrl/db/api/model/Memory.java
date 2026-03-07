package org.ctrl.db.api.model;

public class Memory {

    private final Integer id;
    private final Integer deviceId;
    private final String name;
    private final Integer address;

    public Memory(Integer id, Integer deviceId, String name, Integer address) {
        this.id = id;
        this.deviceId = deviceId;
        this.name = name;
        this.address = address;
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

    public Integer getAddress() {
        return address;
    }
}
