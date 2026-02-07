package org.ctrl.db.controller;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.RrValue;
import org.ctrl.db.service.RrValueService;

public class RrValueController {

    private final RrValueService service;

    public RrValueController(RrValueService service) {
        this.service = service;
    }

    public Optional<RrValue> getCurrent(DeviceInfo device, int address, int bit) {
        return service.getCurrent(device, address, bit);
    }

    public List<RrValue> getRangeCurrent(DeviceInfo device, int address, int startBit, int endBit) {
        return service.getRangeCurrent(device, address, startBit, endBit);
    }

    public void saveValue(DeviceInfo device, int address, int bit, boolean value) {
        service.saveValue(device, address, bit, value);
    }

    public void saveRange(DeviceInfo device, int address, boolean[] bits, int startBit) {
        service.saveRange(device, address, bits, startBit);
    }
}
