package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.MiniFabrica;
import org.ctrl.db.api.repository.MiniFabricaRepository;
import org.springframework.stereotype.Service;

@Service
public class MiniFabricaService {

    private final MiniFabricaRepository repository;

    public MiniFabricaService(MiniFabricaRepository repository) {
        this.repository = repository;
    }

    public List<MiniFabrica> findAll() {
        return repository.findAll();
    }

    public Optional<MiniFabrica> findById(long id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public MiniFabrica create(String name, Long fabricaId) {
        return repository.create(requireName(name), requireId(fabricaId, "fabricaId"));
    }

    public Optional<MiniFabrica> update(long id, String name, Long fabricaId) {
        validateId(id, "id");
        return repository.update(id, requireName(name), requireId(fabricaId, "fabricaId"));
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

    private long requireId(Long id, String fieldName) {
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
