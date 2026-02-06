package org.ctrl.db.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DmValue;
import org.ctrl.db.repository.DmValueRepository;

public class DmValueService {

    private final DmValueRepository repository;

    public DmValueService(DmValueRepository repository) {
        this.repository = repository;
    }

    public Optional<DmValue> getByAddress(int address) {
        return repository.findByAddress(address);
    }

    public List<DmValue> getRange(int startAddress, int endAddress) {
        return repository.findRange(startAddress, endAddress);
    }

    public void saveValue(int address, int value) {
        repository.upsert(new DmValue(address, value, Instant.now()));
    }

    public void saveRange(int startAddress, int[] values) {
        List<DmValue> batch = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            int addr = startAddress + i;
            batch.add(new DmValue(addr, values[i], Instant.now()));
        }
        repository.upsertBatch(batch);
    }

    public void saveBatch(List<DmValue> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        repository.upsertBatch(values);
    }
}
