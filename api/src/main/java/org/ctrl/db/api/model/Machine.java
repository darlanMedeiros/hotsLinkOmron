package org.ctrl.db.api.model;

public class Machine {

    private final Long id;
    private final String name;
    private final Integer deviceId;
    private final Long setorId;

    public Machine(Long id, String name, Integer deviceId, Long setorId) {
        this.id = id;
        this.name = name;
        this.deviceId = deviceId;
        this.setorId = setorId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public Long getSetorId() {
        return setorId;
    }
}
