package org.ctrl.db.controller;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DmValue;
import org.ctrl.db.service.DmValueService;

public class DmValueController {

    private final DmValueService service;

    public DmValueController(DmValueService service) {
        this.service = service;
    }

    public Optional<DmValue> getByAddress(int address) {
        return service.getByAddress(address);
    }

    public List<DmValue> getRange(int startAddress, int endAddress) {
        return service.getRange(startAddress, endAddress);
    }

    public void saveValue(int address, int value) {
        service.saveValue(address, value);
    }

    public void saveRange(int startAddress, int[] values) {
        service.saveRange(startAddress, values);
    }
}
