package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.ctrl.db.model.DmValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcDmValueRepository implements DmValueRepository {

    private static final String SQL_UPSERT_DEVICE =
            "INSERT INTO public.device (mnemonic, name, description) " +
            "VALUES (:mnemonic, :name, :description) " +
            "ON CONFLICT (mnemonic) DO UPDATE " +
            "SET name = EXCLUDED.name, description = EXCLUDED.description " +
            "RETURNING id";

    private static final String SQL_UPSERT_MEMORY =
            "INSERT INTO public.memory (device_id, name) " +
            "VALUES (:deviceId, :name) " +
            "ON CONFLICT (device_id, name) DO UPDATE " +
            "SET name = EXCLUDED.name " +
            "RETURNING id";

    private static final String SQL_INSERT_VALUE =
            "INSERT INTO public.memory_value (memory_id, value, status, updated_at) " +
            "VALUES (:memoryId, :value, :status, :updatedAt)";

    private static final String SQL_UPSERT_CURRENT =
            "INSERT INTO public.memory_value_current (memory_id, value, status, updated_at) " +
            "VALUES (:memoryId, :value, :status, :updatedAt) " +
            "ON CONFLICT (memory_id) DO UPDATE " +
            "SET value = EXCLUDED.value, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at";

    private static final String SQL_FIND_ONE =
            "SELECT m.name, mv.value, mv.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value mv ON mv.memory_id = m.id " +
            "WHERE d.mnemonic = ? AND m.name = ? " +
            "ORDER BY mv.updated_at DESC " +
            "LIMIT 1";

    private static final String SQL_FIND_RANGE =
            "SELECT DISTINCT ON (m.id) m.name, mv.value, mv.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value mv ON mv.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND m.name IN (:names) " +
            "ORDER BY m.id, mv.updated_at DESC";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final Map<String, Integer> memoryIdCache = new HashMap<>();
    private Integer cachedDeviceId = null;
    private String cachedMnemonic = null;

    public JdbcDmValueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Optional<DmValue> findByAddress(String deviceMnemonic, int address) {
        String name = formatDmName(address);
        List<DmValue> rows = jdbcTemplate.query(SQL_FIND_ONE, mapper(), deviceMnemonic, name);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Override
    public List<DmValue> findRange(String deviceMnemonic, int startAddress, int endAddress) {
        List<String> names = buildDmNames(startAddress, endAddress);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", deviceMnemonic)
                .addValue("names", names);
        return namedTemplate.query(SQL_FIND_RANGE, params, mapper());
    }

    @Override
    public void upsert(String deviceMnemonic, String deviceName, String deviceDescription, DmValue value) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        int memoryId = ensureMemory(deviceId, formatDmName(value.getAddress()));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memoryId", memoryId)
                .addValue("value", value.getValue())
                .addValue("status", true)
                .addValue("updatedAt", Timestamp.from(value.getUpdatedAt()));
        namedTemplate.update(SQL_INSERT_VALUE, params);
        namedTemplate.update(SQL_UPSERT_CURRENT, params);
    }

    @Override
    public void upsertBatch(String deviceMnemonic, String deviceName, String deviceDescription, List<DmValue> values) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        MapSqlParameterSource[] batch = new MapSqlParameterSource[values.size()];
        for (int i = 0; i < values.size(); i++) {
            DmValue value = values.get(i);
            int memoryId = ensureMemory(deviceId, formatDmName(value.getAddress()));
            batch[i] = new MapSqlParameterSource()
                    .addValue("memoryId", memoryId)
                    .addValue("value", value.getValue())
                    .addValue("status", true)
                    .addValue("updatedAt", Timestamp.from(value.getUpdatedAt()));
        }
        namedTemplate.batchUpdate(SQL_INSERT_VALUE, batch);
        namedTemplate.batchUpdate(SQL_UPSERT_CURRENT, batch);
    }

    private RowMapper<DmValue> mapper() {
        return (ResultSet rs, int rowNum) -> mapRow(rs);
    }

    private DmValue mapRow(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        int address = parseDmAddress(name);
        int value = rs.getInt("value");
        Timestamp ts = rs.getTimestamp("updated_at");
        Instant updatedAt = ts == null ? Instant.now() : ts.toInstant();
        return new DmValue(address, value, updatedAt);
    }

    private int ensureDevice(String mnemonic, String name, String description) {
        if (cachedDeviceId != null && mnemonic.equals(cachedMnemonic)) {
            return cachedDeviceId;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", mnemonic)
                .addValue("name", name)
                .addValue("description", description);
        Integer id = namedTemplate.queryForObject(SQL_UPSERT_DEVICE, params, Integer.class);
        cachedDeviceId = id;
        cachedMnemonic = mnemonic;
        memoryIdCache.clear();
        return id;
    }

    private int ensureMemory(int deviceId, String name) {
        Integer cached = memoryIdCache.get(name);
        if (cached != null) {
            return cached;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("deviceId", deviceId)
                .addValue("name", name);
        Integer id = namedTemplate.queryForObject(SQL_UPSERT_MEMORY, params, Integer.class);
        memoryIdCache.put(name, id);
        return id;
    }

    private String formatDmName(int address) {
        return String.format("DM_%04d", address);
    }

    private int parseDmAddress(String name) {
        if (name == null || !name.startsWith("DM_")) {
            return 0;
        }
        try {
            return Integer.parseInt(name.substring(3));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private List<String> buildDmNames(int startAddress, int endAddress) {
        int size = Math.max(0, endAddress - startAddress + 1);
        java.util.ArrayList<String> names = new java.util.ArrayList<>(size);
        for (int addr = startAddress; addr <= endAddress; addr++) {
            names.add(formatDmName(addr));
        }
        return names;
    }
}
