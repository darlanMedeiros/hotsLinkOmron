package org.ctrl.db.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.DmValue;
import org.ctrl.db.repository.DmValueRepository;

public class DmValueService {

    private final DmValueRepository repository;

    public DmValueService(DmValueRepository repository) {
        this.repository = repository;
    }

    public Optional<DmValue> getByAddress(DeviceInfo device, int address) {
        return repository.findByAddress(device.getMnemonic(), address);
    }

    public List<DmValue> getRange(DeviceInfo device, int startAddress, int endAddress) {
        return repository.findRange(device.getMnemonic(), startAddress, endAddress);
    }

    public void saveValue(DeviceInfo device, int address, int value) {
        repository.upsert(device.getMnemonic(), device.getName(), device.getDescription(),
                new DmValue(address, value, Instant.now()));
    }

    public void saveRange(DeviceInfo device, int startAddress, int[] values) {
        List<DmValue> batch = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            int addr = startAddress + i;
            batch.add(new DmValue(addr, values[i], Instant.now()));
        }
        repository.upsertBatch(device.getMnemonic(), device.getName(), device.getDescription(), batch);
    }

    public void saveBatch(DeviceInfo device, List<DmValue> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        repository.upsertBatch(device.getMnemonic(), device.getName(), device.getDescription(), values);
    }
}
