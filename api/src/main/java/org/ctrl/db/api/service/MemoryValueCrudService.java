package org.ctrl.db.api.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.MemoryValueCrud;
import org.ctrl.db.api.repository.MemoryValueCrudRepository;
import org.springframework.stereotype.Service;

@Service
public class MemoryValueCrudService {

    private final MemoryValueCrudRepository repository;

    public MemoryValueCrudService(MemoryValueCrudRepository repository) {
        this.repository = repository;
    }

    public List<MemoryValueCrud> findAll() {
        return repository.findAll();
    }

    public Optional<MemoryValueCrud> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public MemoryValueCrud create(Integer memoryId, Integer value, Boolean status, String updatedAt) {
        return repository.create(
                requireId(memoryId, "memoryId"),
                requireInt(value, "value"),
                requireBoolean(status, "status"),
                parseDateTime(updatedAt));
    }

    public Optional<MemoryValueCrud> update(int id, Integer memoryId, Integer value, Boolean status, String updatedAt) {
        validateId(id, "id");
        return repository.update(
                id,
                requireId(memoryId, "memoryId"),
                requireInt(value, "value"),
                requireBoolean(status, "status"),
                parseDateTime(updatedAt));
    }

    public boolean delete(int id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String text = raw.trim();
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(text).toLocalDateTime();
            } catch (DateTimeParseException ex2) {
                throw new IllegalArgumentException("updatedAt must be a valid ISO date-time");
            }
        }
    }

    private int requireId(Integer id, String field) {
        if (id == null || id.intValue() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
        return id.intValue();
    }

    private int requireInt(Integer value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.intValue();
    }

    private boolean requireBoolean(Boolean value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.booleanValue();
    }

    private void validateId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
