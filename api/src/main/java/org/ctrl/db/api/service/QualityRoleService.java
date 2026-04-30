package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.QualityRoleOption;
import org.ctrl.db.api.repository.QualityRoleRepository;
import org.springframework.stereotype.Service;

@Service
public class QualityRoleService {

    private final QualityRoleRepository repository;

    public QualityRoleService(QualityRoleRepository repository) {
        this.repository = repository;
    }

    public List<QualityRoleOption> findAll() {
        return repository.findAll();
    }

    public List<QualityRoleOption> findActive() {
        return repository.findActive();
    }

    public Optional<QualityRoleOption> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public QualityRoleOption create(String name, String description, Boolean active) {
        return repository.create(requireName(name), normalizeDescription(description), resolveActive(active));
    }

    public Optional<QualityRoleOption> update(int id, String name, String description, Boolean active) {
        validateId(id, "id");
        return repository.update(id, requireName(name), normalizeDescription(description), resolveActive(active));
    }

    public boolean delete(int id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private void validateId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return name.trim().toUpperCase();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean resolveActive(Boolean active) {
        return active == null ? true : active.booleanValue();
    }
}

