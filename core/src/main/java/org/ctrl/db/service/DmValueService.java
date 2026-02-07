package org.ctrl.db.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.db.repository.MemoryValueRepository;

public class DmValueService {

    private final MemoryValueRepository repository;

    public DmValueService(MemoryValueRepository repository) {
        this.repository = repository;
    }

    public Optional<MemoryValue> getByAddress(DeviceInfo device, int address) {
        String name = formatDmName(address);
        return repository.findLatestByName(device.getMnemonic(), name);
    }

    public Optional<MemoryValue> getCurrentByAddress(DeviceInfo device, int address) {
        String name = formatDmName(address);
        return repository.findCurrentByName(device.getMnemonic(), name);
    }

    public List<MemoryValue> getRange(DeviceInfo device, int startAddress, int endAddress) {
        List<String> names = buildDmNames(startAddress, endAddress);
        List<MemoryValue> rows = repository.findRangeLatestByNames(device.getMnemonic(), names);
        rows.sort(Comparator.comparingInt(row -> parseDmAddress(row.getName())));
        return rows;
    }

    public Optional<MemoryValue> getLatest(DeviceInfo device) {
        return repository.findLatestCurrentByDevice(device.getMnemonic());
    }

    public void saveValue(DeviceInfo device, int address, int value) {
        String name = formatDmName(address);
        repository.upsert(device.getMnemonic(), device.getName(), device.getDescription(),
                new MemoryValue(name, value, LocalDateTime.now()));
    }

    public void saveRange(DeviceInfo device, int startAddress, int[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        List<MemoryValue> batch = new ArrayList<>(values.length);
        for (int i = 0; i < values.length; i++) {
            int addr = startAddress + i;
            batch.add(new MemoryValue(formatDmName(addr), values[i], LocalDateTime.now()));
        }
        repository.upsertBatch(device.getMnemonic(), device.getName(), device.getDescription(), batch);
    }

    public void saveBatch(DeviceInfo device, List<MemoryValue> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        repository.upsertBatch(device.getMnemonic(), device.getName(), device.getDescription(), values);
    }

    public static String formatDmName(int address) {
        return String.format("DM_%04d", address);
    }

    public static int parseDmAddress(String name) {
        if (name == null || !name.startsWith("DM_")) {
            return 0;
        }
        try {
            return Integer.parseInt(name.substring(3));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static MemoryValue buildDmValue(int address, int value, LocalDateTime updatedAt) {
        return new MemoryValue(formatDmName(address), value, updatedAt);
    }

    private List<String> buildDmNames(int startAddress, int endAddress) {
        int size = Math.max(0, endAddress - startAddress + 1);
        ArrayList<String> names = new ArrayList<>(size);
        for (int addr = startAddress; addr <= endAddress; addr++) {
            names.add(formatDmName(addr));
        }
        return names;
    }
}
