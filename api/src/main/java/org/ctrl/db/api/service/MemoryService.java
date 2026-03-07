package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Memory;
import org.ctrl.db.api.repository.MemoryRepository;
import org.springframework.stereotype.Service;

@Service
public class MemoryService {

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

    public Memory create(Integer deviceId, String name) {
        return repository.create(requireId(deviceId, "deviceId"), requireName(name));
    }

    public Optional<Memory> update(int id, Integer deviceId, String name) {
        validateId(id, "id");
        return repository.update(id, requireId(deviceId, "deviceId"), requireName(name));
    }

    public boolean delete(int id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return name.trim();
    }

    private int requireId(Integer id, String field) {
        if (id == null || id.intValue() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return id.intValue();
    }

    private void validateId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
