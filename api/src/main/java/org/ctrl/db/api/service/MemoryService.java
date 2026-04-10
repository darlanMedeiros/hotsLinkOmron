package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.ctrl.db.api.model.Memory;
import org.ctrl.db.api.repository.MemoryRepository;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

    private static final Set<String> ALLOWED_AREAS = Set.of("DM", "HR", "RR", "WR", "TC");

    private final MemoryRepository repository;

    public MemoryService(MemoryRepository repository) {
        this.repository = repository;
    }

    public List<Memory> findAll() {
        return repository.findAll();
    }

    public Optional<Memory> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public Memory create(Integer deviceId, String area, Integer address, Integer bit) {
        return repository.create(
                requireId(deviceId, "deviceId"),
                requireArea(area),
                requireAddress(address),
                requireBit(bit));
    }

    public Optional<Memory> update(int id, Integer deviceId, String area, Integer address, Integer bit) {
        validateId(id, "id");
        return repository.update(
                id,
                requireId(deviceId, "deviceId"),
                requireArea(area),
                requireAddress(address),
                requireBit(bit));
    }

    public boolean delete(int id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private String requireArea(String area) {
        if (area == null || area.trim().isEmpty()) {
            throw new IllegalArgumentException("area is required");
        }
        String normalized = area.trim().toUpperCase();
        if (!ALLOWED_AREAS.contains(normalized)) {
            throw new IllegalArgumentException("area must be one of DM, HR, RR, WR, TC");
        }
        return normalized;
    }

    private int requireId(Integer id, String field) {
        if (id == null || id.intValue() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return id.intValue();
    }

    private int requireAddress(Integer address) {
        if (address == null || address.intValue() < 0) {
            throw new IllegalArgumentException("address must be zero or greater");
        }
        return address.intValue();
    }

    private int requireBit(Integer bit) {
        if (bit == null) {
            return -1;
        }
        int normalized = bit.intValue();
        if (normalized < -1 || normalized > 15) {
            throw new IllegalArgumentException("bit must be between 0 and 15, or -1 for word access");
        }
        return normalized;
    }

    private void validateId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
