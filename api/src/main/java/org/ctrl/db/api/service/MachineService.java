package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Machine;
import org.ctrl.db.api.repository.MachineRepository;
import org.springframework.stereotype.Service;

@Service
public class MachineService {

    private final MachineRepository repository;

    public MachineService(MachineRepository repository) {
        this.repository = repository;
    }

    public List<Machine> findAll() {
        return repository.findAll();
    }

    public Optional<Machine> findById(long id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public Machine create(String name, Integer deviceId, Long setorId) {
        return repository.create(
                requireName(name),
                requireDeviceId(deviceId),
                requireLongId(setorId, "setorId"));
    }

    public Optional<Machine> update(long id, String name, Integer deviceId, Long setorId) {
        validateId(id, "id");
        return repository.update(
                id,
                requireName(name),
                requireDeviceId(deviceId),
                requireLongId(setorId, "setorId"));
    }

    public boolean delete(long id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return name.trim();
    }

    private int requireDeviceId(Integer id) {
        if (id == null || id.intValue() <= 0) {
            throw new IllegalArgumentException("deviceId must be greater than zero");
        }
        return id.intValue();
    }

    private long requireLongId(Long id, String fieldName) {
        if (id == null || id.longValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return id.longValue();
    }

    private void validateId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
