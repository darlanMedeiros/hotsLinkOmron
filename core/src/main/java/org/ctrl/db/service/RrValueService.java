package org.ctrl.db.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.RrValue;
import org.ctrl.db.repository.RrValueRepository;

public class RrValueService {

    private final RrValueRepository repository;

    public RrValueService(RrValueRepository repository) {
        this.repository = repository;
    }

    public Optional<RrValue> getCurrent(DeviceInfo device, int address, int bit) {
        return repository.findCurrentByAddressBit(device.getMnemonic(), address, bit);
    }

    public List<RrValue> getRangeCurrent(DeviceInfo device, int address, int startBit, int endBit) {
        return repository.findRangeCurrent(device.getMnemonic(), address, startBit, endBit);
    }

    public void saveValue(DeviceInfo device, int address, int bit, boolean value) {
        repository.upsert(device.getMnemonic(), device.getName(), device.getDescription(),
                new RrValue(address, bit, value, Instant.now()));
    }

    public void saveRange(DeviceInfo device, int address, boolean[] bits, int startBit) {
        if (bits == null || bits.length == 0) {
            return;
        }
        List<RrValue> batch = new ArrayList<>();
        for (int i = 0; i < bits.length; i++) {
            int bit = startBit + i;
            if (bit < 0 || bit > 15) {
                continue;
            }
            batch.add(new RrValue(address, bit, bits[i], Instant.now()));
        }
        if (!batch.isEmpty()) {
            repository.upsertBatch(device.getMnemonic(), device.getName(), device.getDescription(), batch);
        }
    }
}
