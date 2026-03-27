package org.ctrl.db.repository;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.Tag;

public interface TagRepository {

    Tag create(String name, long machineId, int memoryId);

    Optional<Tag> findById(int id);

    Optional<Tag> findByDeviceAndName(int deviceId, String name);

    Optional<Tag> findByMemoryId(int memoryId);

    List<Tag> findByDeviceId(int deviceId);
}
