package org.ctrl.db.api.service;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Device;
import org.ctrl.db.api.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public List<Device> findAll() {
        return repository.findAll();
    }

    public Optional<Device> findById(int id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    @Transactional
    public Device create(String mnemonic, String name, String description, Integer nodeId) {
        return repository.create(
                requireMnemonic(mnemonic),
                requireName(name),
                normalizeText(description),
                normalizeNodeId(nodeId));
    }

    @Transactional
    public Optional<Device> update(int id, String mnemonic, String name, String description, Integer nodeId) {
        validateId(id, "id");
        return repository.update(
                id,
                requireMnemonic(mnemonic),
                requireName(name),
                normalizeText(description),
                normalizeNodeId(nodeId));
    }

    public boolean delete(int id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private String requireMnemonic(String mnemonic) {
        if (mnemonic == null || mnemonic.trim().isEmpty()) {
            throw new IllegalArgumentException("mnemonic is required");
        }
        return mnemonic.trim();
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return name.trim();
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private Integer normalizeNodeId(Integer nodeId) {
        if (nodeId == null) {
            return null;
        }
        if (nodeId.intValue() < 0) {
            throw new IllegalArgumentException("nodeId must be greater than or equal to zero");
        }
        return nodeId;
    }

    private void validateId(int id, String field) {
        if (id <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}
