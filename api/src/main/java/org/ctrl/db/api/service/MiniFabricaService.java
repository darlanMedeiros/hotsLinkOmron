package org.ctrl.db.api.service;

import java.util.ArrayList;
import java.util.Comparator;
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

    public MiniFabrica create(String name, Long fabricaId, List<Long> setorIds) {
        return repository.create(requireName(name), requireId(fabricaId, "fabricaId"), normalizeIds(setorIds, "setorIds"));
    }

    public Optional<MiniFabrica> update(long id, String name, Long fabricaId, List<Long> setorIds) {
        validateId(id, "id");
        return repository.update(id, requireName(name), requireId(fabricaId, "fabricaId"), normalizeIds(setorIds, "setorIds"));
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

    private List<Long> normalizeIds(List<Long> ids, String fieldName) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (Long id : ids) {
            if (id == null || id.longValue() <= 0) {
                throw new IllegalArgumentException(fieldName + " must contain only positive ids");
            }
            if (!result.contains(id.longValue())) {
                result.add(id.longValue());
            }
        }
        result.sort(Comparator.naturalOrder());
        return result;
    }

    private void validateId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
