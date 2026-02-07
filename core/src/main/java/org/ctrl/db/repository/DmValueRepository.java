package org.ctrl.db.repository;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DmValue;

public interface DmValueRepository {

    Optional<DmValue> findByAddress(String deviceMnemonic, int address);

    Optional<DmValue> findCurrentByAddress(String deviceMnemonic, int address);

    List<DmValue> findRange(String deviceMnemonic, int startAddress, int endAddress);

    Optional<DmValue> findLatest(String deviceMnemonic);

    void upsert(String deviceMnemonic, String deviceName, String deviceDescription, DmValue value);

    void upsertBatch(String deviceMnemonic, String deviceName, String deviceDescription, List<DmValue> values);
}
