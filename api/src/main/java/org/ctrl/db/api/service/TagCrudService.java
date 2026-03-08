package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Memory;
import org.ctrl.db.api.model.TagCrud;
import org.ctrl.db.api.repository.MemoryRepository;
import org.ctrl.db.api.repository.TagCrudRepository;
import org.springframework.stereotype.Service;

@Service
public class TagCrudService {

    private final TagCrudRepository repository;
    private final MemoryRepository memoryRepository;

    public TagCrudService(TagCrudRepository repository, MemoryRepository memoryRepository) {
        this.repository = repository;
        this.memoryRepository = memoryRepository;
    }

    public List<TagCrud> findAll() {
        return repository.findAll();
    }

    public Optional<TagCrud> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public TagCrud create(String name, Integer deviceId, Integer memoryId, Boolean persistHistory) {
        int validatedDeviceId = requireId(deviceId, "deviceId");
        int validatedMemoryId = requireId(memoryId, "memoryId");
        ensureMemoryBelongsToDevice(validatedMemoryId, validatedDeviceId);
        return repository.create(requireName(name), validatedDeviceId, validatedMemoryId, resolvePersistHistory(persistHistory));
    }

    public Optional<TagCrud> update(int id, String name, Integer deviceId, Integer memoryId, Boolean persistHistory) {
        validateId(id, "id");
        int validatedDeviceId = requireId(deviceId, "deviceId");
        int validatedMemoryId = requireId(memoryId, "memoryId");
        ensureMemoryBelongsToDevice(validatedMemoryId, validatedDeviceId);
        return repository.update(id, requireName(name), validatedDeviceId, validatedMemoryId,
                resolvePersistHistory(persistHistory));
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

    private void ensureMemoryBelongsToDevice(int memoryId, int deviceId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("memoryId does not exist"));
        if (memory.getDeviceId().intValue() != deviceId) {
            throw new IllegalArgumentException("memoryId must belong to the informed deviceId");
        }
    }

    private boolean resolvePersistHistory(Boolean persistHistory) {
        return persistHistory == null ? true : persistHistory.booleanValue();
    }
}
