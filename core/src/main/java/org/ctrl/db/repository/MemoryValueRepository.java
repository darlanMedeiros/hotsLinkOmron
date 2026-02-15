package org.ctrl.db.repository;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.MemoryValue;

public interface MemoryValueRepository {

    Optional<MemoryValue> findLatestByName(String deviceMnemonic, String name);

    Optional<MemoryValue> findCurrentByName(String deviceMnemonic, String name);

    List<MemoryValue> findRangeLatestByNames(String deviceMnemonic, List<String> names);

    List<MemoryValue> findRangeCurrentByNames(String deviceMnemonic, List<String> names);

    Optional<MemoryValue> findLatestCurrentByDevice(String deviceMnemonic);

    void upsert(String deviceMnemonic, String deviceName, String deviceDescription, MemoryValue value);

    void upsertBatch(String deviceMnemonic, String deviceName, String deviceDescription, List<MemoryValue> values);

    int pruneHistoryOlderThanDays(int days);
}
