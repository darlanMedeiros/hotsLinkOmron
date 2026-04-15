package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Defeito;
import org.ctrl.db.api.repository.DefeitoRepository;
import org.springframework.stereotype.Service;

@Service
public class DefeitoService {

    private final DefeitoRepository repository;

    public DefeitoService(DefeitoRepository repository) {
        this.repository = repository;
    }

    public List<Defeito> findAll() {
        return repository.findAll();
    }

    public Optional<Defeito> findById(long id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public Defeito create(String name, Integer number) {
        return repository.create(requireName(name), number);
    }

    public Optional<Defeito> update(long id, String name, Integer number) {
        validateId(id, "id");
        return repository.update(id, requireName(name), number);
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
