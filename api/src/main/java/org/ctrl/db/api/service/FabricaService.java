package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Fabrica;
import org.ctrl.db.api.repository.FabricaRepository;
import org.springframework.stereotype.Service;

@Service
public class FabricaService {

    private final FabricaRepository repository;

    public FabricaService(FabricaRepository repository) {
        this.repository = repository;
    }

    public List<Fabrica> findAll() {
        return repository.findAll();
    }

    public Optional<Fabrica> findById(long id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public Fabrica create(String name) {
        return repository.create(requireName(name));
    }

    public Optional<Fabrica> update(long id, String name) {
        validateId(id, "id");
        return repository.update(id, requireName(name));
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

    private void validateId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
