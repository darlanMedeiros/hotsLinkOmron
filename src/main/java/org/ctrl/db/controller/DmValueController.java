package org.ctrl.db.controller;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.DmValue;
import org.ctrl.db.service.DmValueService;

public class DmValueController {

    private final DmValueService service;

    public DmValueController(DmValueService service) {
        this.service = service;
    }

    public Optional<DmValue> getByAddress(DeviceInfo device, int address) {
        return service.getByAddress(device, address);
    }

    public List<DmValue> getRange(DeviceInfo device, int startAddress, int endAddress) {
        return service.getRange(device, startAddress, endAddress);
    }

    public void saveValue(DeviceInfo device, int address, int value) {
        service.saveValue(device, address, value);
    }

    public void saveRange(DeviceInfo device, int startAddress, int[] values) {
        service.saveRange(device, startAddress, values);
    }
}
