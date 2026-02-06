package org.ctrl.db.repository;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DmValue;

public interface DmValueRepository {

    Optional<DmValue> findByAddress(int address);

    List<DmValue> findRange(int startAddress, int endAddress);

    void upsert(DmValue value);

    void upsertBatch(List<DmValue> values);
}
