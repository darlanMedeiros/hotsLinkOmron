package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Setor;
import org.ctrl.db.api.repository.SetorRepository;
import org.springframework.stereotype.Service;

@Service
public class SetorService {

    private final SetorRepository repository;

    public SetorService(SetorRepository repository) {
        this.repository = repository;
    }

    public List<Setor> findAll() {
        return repository.findAll();
    }

    public Optional<Setor> findById(long id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public Setor create(String name, Long miniFabricaId) {
        return repository.create(requireName(name), requireId(miniFabricaId, "miniFabricaId"));
    }

    public Optional<Setor> update(long id, String name, Long miniFabricaId) {
        validateId(id, "id");
        return repository.update(id, requireName(name), requireId(miniFabricaId, "miniFabricaId"));
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
