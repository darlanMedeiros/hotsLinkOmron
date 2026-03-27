package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Machine;
import org.ctrl.db.api.model.Memory;
import org.ctrl.db.api.model.TagCrud;
import org.ctrl.db.api.repository.MachineRepository;
import org.ctrl.db.api.repository.MemoryRepository;
import org.ctrl.db.api.repository.TagCrudRepository;
import org.springframework.stereotype.Service;

@Service
public class TagCrudService {

    private final TagCrudRepository repository;
    private final MemoryRepository memoryRepository;
    private final MachineRepository machineRepository;

    public TagCrudService(TagCrudRepository repository, MemoryRepository memoryRepository, MachineRepository machineRepository) {
        this.repository = repository;
        this.memoryRepository = memoryRepository;
        this.machineRepository = machineRepository;
    }

    public List<TagCrud> findAll() {
        return repository.findAll();
    }

    public Optional<TagCrud> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public TagCrud create(String name, Long machineId, Integer memoryId, Boolean persistHistory) {
        long validatedMachineId = requireLongId(machineId, "machineId");
        int validatedMemoryId = requireId(memoryId, "memoryId");
        ensureMemoryBelongsToMachineDevice(validatedMemoryId, validatedMachineId);
        return repository.create(requireName(name), validatedMachineId, validatedMemoryId, resolvePersistHistory(persistHistory));
    }

    public Optional<TagCrud> update(int id, String name, Long machineId, Integer memoryId, Boolean persistHistory) {
        validateId(id, "id");
        long validatedMachineId = requireLongId(machineId, "machineId");
        int validatedMemoryId = requireId(memoryId, "memoryId");
        ensureMemoryBelongsToMachineDevice(validatedMemoryId, validatedMachineId);
        return repository.update(id, requireName(name), validatedMachineId, validatedMemoryId,
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

    private long requireLongId(Long id, String field) {
        if (id == null || id.longValue() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return id.longValue();
    }

    private void validateId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }

    private void ensureMemoryBelongsToMachineDevice(int memoryId, long machineId) {
        Memory memory = memoryRepository.findById(memoryId)
                .orElseThrow(() -> new IllegalArgumentException("memoryId does not exist"));
        Machine machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new IllegalArgumentException("machineId does not exist"));

        if (memory.getDeviceId().intValue() != machine.getDeviceId().intValue()) {
            throw new IllegalArgumentException("memoryId must belong to the same device configured in machineId");
        }
    }

    private boolean resolvePersistHistory(Boolean persistHistory) {
        return persistHistory == null ? true : persistHistory.booleanValue();
    }
}
