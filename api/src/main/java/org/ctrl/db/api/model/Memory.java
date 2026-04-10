package org.ctrl.db.api.model;

public class Memory {

    private final Integer id;
    private final Integer deviceId;
    private final String area;
    private final Integer address;
    private final Integer bit;

    public Memory(Integer id, Integer deviceId, String area, Integer address, Integer bit) {
        this.id = id;
        this.deviceId = deviceId;
        this.area = area;
        this.address = address;
        this.bit = bit;
    }

    public Integer getId() {
        return id;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public String getArea() {
        return area;
    }

    public Integer getAddress() {
        return address;
    }

    public Integer getBit() {
        return bit;
    }
}
