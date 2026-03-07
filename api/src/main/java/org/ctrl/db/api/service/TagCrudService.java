package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.TagCrud;
import org.ctrl.db.api.repository.TagCrudRepository;
import org.springframework.stereotype.Service;

@Service
public class TagCrudService {

    private final TagCrudRepository repository;

    public TagCrudService(TagCrudRepository repository) {
        this.repository = repository;
    }

    public List<TagCrud> findAll() {
        return repository.findAll();
    }

    public Optional<TagCrud> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public TagCrud create(String name, Integer deviceId, Integer memoryId) {
        return repository.create(requireName(name), requireId(deviceId, "deviceId"), requireId(memoryId, "memoryId"));
    }

    public Optional<TagCrud> update(int id, String name, Integer deviceId, Integer memoryId) {
        validateId(id, "id");
        return repository.update(id, requireName(name), requireId(deviceId, "deviceId"), requireId(memoryId, "memoryId"));
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
