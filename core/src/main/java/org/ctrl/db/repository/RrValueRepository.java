package org.ctrl.db.repository;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.RrValue;

public interface RrValueRepository {

    Optional<RrValue> findCurrentByAddressBit(String deviceMnemonic, int address, int bit);

    List<RrValue> findRangeCurrent(String deviceMnemonic, int address, int startBit, int endBit);

    void upsert(String deviceMnemonic, String deviceName, String deviceDescription, RrValue value);

    void upsertBatch(String deviceMnemonic, String deviceName, String deviceDescription, List<RrValue> values);
}
