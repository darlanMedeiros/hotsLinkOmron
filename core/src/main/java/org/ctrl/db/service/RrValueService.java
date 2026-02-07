package org.ctrl.db.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.db.repository.MemoryValueRepository;

public class RrValueService {

    private final MemoryValueRepository repository;

    public RrValueService(MemoryValueRepository repository) {
        this.repository = repository;
    }

    public Optional<MemoryValue> getCurrent(DeviceInfo device, int address, int bit) {
        String name = formatRrName(address, bit);
        return repository.findCurrentByName(device.getMnemonic(), name);
    }

    public List<MemoryValue> getRangeCurrent(DeviceInfo device, int address, int startBit, int endBit) {
        List<String> names = buildRrNames(address, startBit, endBit);
        return repository.findRangeCurrentByNames(device.getMnemonic(), names);
    }

    public void saveValue(DeviceInfo device, int address, int bit, boolean value) {
        String name = formatRrName(address, bit);
        repository.upsert(device.getMnemonic(), device.getName(), device.getDescription(),
                new MemoryValue(name, value ? 1 : 0, LocalDateTime.now()));
    }

    public void saveRange(DeviceInfo device, int address, boolean[] bits, int startBit) {
        if (bits == null || bits.length == 0) {
            return;
        }
        List<MemoryValue> batch = new ArrayList<>();
        for (int i = 0; i < bits.length; i++) {
            int bit = startBit + i;
            if (bit < 0 || bit > 15) {
                continue;
            }
            batch.add(new MemoryValue(formatRrName(address, bit), bits[i] ? 1 : 0, LocalDateTime.now()));
        }
        if (!batch.isEmpty()) {
            repository.upsertBatch(device.getMnemonic(), device.getName(), device.getDescription(), batch);
        }
    }

    public static String formatRrName(int address, int bit) {
        return String.format("RR_%04d.%02d", address, bit);
    }

    public static Parsed parseRrName(String name) {
        if (name == null || !name.startsWith("RR_")) {
            return new Parsed(0, 0);
        }
        String rest = name.substring(3);
        int dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1) {
            return new Parsed(0, 0);
        }
        try {
            int address = Integer.parseInt(rest.substring(0, dot));
            int bit = Integer.parseInt(rest.substring(dot + 1));
            return new Parsed(address, bit);
        } catch (NumberFormatException ex) {
            return new Parsed(0, 0);
        }
    }

    private List<String> buildRrNames(int address, int startBit, int endBit) {
        int s = Math.max(0, startBit);
        int e = Math.min(15, endBit);
        int size = Math.max(0, e - s + 1);
        ArrayList<String> names = new ArrayList<>(size);
        for (int bit = s; bit <= e; bit++) {
            names.add(formatRrName(address, bit));
        }
        return names;
    }

    public static final class Parsed {
        private final int address;
        private final int bit;

        private Parsed(int address, int bit) {
            this.address = address;
            this.bit = bit;
        }

        public int getAddress() {
            return address;
        }

        public int getBit() {
            return bit;
        }
    }

    public static MemoryValue buildRrValue(int address, int bit, boolean value, LocalDateTime updatedAt) {
        return new MemoryValue(formatRrName(address, bit), value ? 1 : 0, updatedAt);
    }
}
