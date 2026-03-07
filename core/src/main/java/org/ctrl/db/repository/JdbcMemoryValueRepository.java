package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.model.MemoryValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;

public class JdbcMemoryValueRepository implements MemoryValueRepository {

    private static final String SQL_UPSERT_DEVICE =
            "INSERT INTO public.device (mnemonic, name, description) " +
            "VALUES (:mnemonic, :name, :description) " +
            "ON CONFLICT (mnemonic) DO UPDATE " +
            "SET name = EXCLUDED.name, description = EXCLUDED.description " +
            "RETURNING id";

    private static final String SQL_UPSERT_MEMORY =
            "INSERT INTO public.memory (device_id, name, address) " +
            "VALUES (:deviceId, :name, :address) " +
            "ON CONFLICT (device_id, name) DO UPDATE " +
            "SET name = EXCLUDED.name, address = EXCLUDED.address " +
            "RETURNING id";

    private static final String SQL_INSERT_VALUE =
            "INSERT INTO public.memory_value (memory_id, value, status, updated_at) " +
            "VALUES (:memoryId, :value, :status, :updatedAt)";

    private static final String SQL_UPSERT_CURRENT =
            "INSERT INTO public.memory_value_current (memory_id, value, status, updated_at) " +
            "VALUES (:memoryId, :value, :status, :updatedAt) " +
            "ON CONFLICT (memory_id) DO UPDATE " +
            "SET value = EXCLUDED.value, status = EXCLUDED.status, updated_at = EXCLUDED.updated_at";

    private static final String SQL_FIND_LATEST_BY_NAME =
            "SELECT m.name, mv.value, mv.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value mv ON mv.memory_id = m.id " +
            "WHERE d.mnemonic = ? AND m.name = ? " +
            "ORDER BY mv.updated_at DESC " +
            "LIMIT 1";

    private static final String SQL_FIND_CURRENT_BY_NAME =
            "SELECT m.name, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = ? AND m.name = ? " +
            "LIMIT 1";

    private static final String SQL_FIND_RANGE_LATEST =
            "SELECT DISTINCT ON (m.id) m.name, mv.value, mv.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value mv ON mv.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND m.name IN (:names) " +
            "ORDER BY m.id, mv.updated_at DESC";

    private static final String SQL_FIND_RANGE_CURRENT =
            "SELECT m.name, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND m.name IN (:names) " +
            "ORDER BY m.name";

    private static final String SQL_FIND_LATEST_CURRENT =
            "SELECT m.name, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = ? " +
            "ORDER BY mvc.updated_at DESC " +
            "LIMIT 1";

    private static final String SQL_DELETE_HISTORY_OLDER_THAN_DAYS =
            "DELETE FROM public.memory_value " +
            "WHERE updated_at < (NOW() - (:days * INTERVAL '1 day'))";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final @NonNull RowMapper<MemoryValue> rowMapper = (ResultSet rs, int rowNum) -> mapRow(rs);
    private final Map<String, Integer> memoryIdCache = new HashMap<>();
    private Integer cachedDeviceId = null;
    private String cachedMnemonic = null;

    public JdbcMemoryValueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.namedTemplate = new NamedParameterJdbcTemplate(this.jdbcTemplate);
    }

    @Override
    public Optional<MemoryValue> findLatestByName(String deviceMnemonic, String name) {
        List<MemoryValue> rows = jdbcTemplate.query(SQL_FIND_LATEST_BY_NAME, rowMapper, deviceMnemonic, name);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Override
    public Optional<MemoryValue> findCurrentByName(String deviceMnemonic, String name) {
        List<MemoryValue> rows = jdbcTemplate.query(SQL_FIND_CURRENT_BY_NAME, rowMapper, deviceMnemonic, name);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Override
    public List<MemoryValue> findRangeLatestByNames(String deviceMnemonic, List<String> names) {
        if (names == null || names.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", deviceMnemonic)
                .addValue("names", names);
        return namedTemplate.query(SQL_FIND_RANGE_LATEST, params, rowMapper);
    }

    @Override
    public List<MemoryValue> findRangeCurrentByNames(String deviceMnemonic, List<String> names) {
        if (names == null || names.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", deviceMnemonic)
                .addValue("names", names);
        return namedTemplate.query(SQL_FIND_RANGE_CURRENT, params, rowMapper);
    }

    @Override
    public Optional<MemoryValue> findLatestCurrentByDevice(String deviceMnemonic) {
        List<MemoryValue> rows = jdbcTemplate.query(SQL_FIND_LATEST_CURRENT, rowMapper, deviceMnemonic);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Override
    public void upsert(String deviceMnemonic, String deviceName, String deviceDescription, MemoryValue value) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        int memoryId = ensureMemory(deviceId, value.getName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memoryId", memoryId)
                .addValue("value", value.getValue())
                .addValue("status", true)
                .addValue("updatedAt", Timestamp.valueOf(value.getUpdatedAt()));
        namedTemplate.update(SQL_INSERT_VALUE, params);
        namedTemplate.update(SQL_UPSERT_CURRENT, params);
    }

    @Override
    public void upsertBatch(String deviceMnemonic, String deviceName, String deviceDescription,
            List<MemoryValue> values) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        MapSqlParameterSource[] batch = new MapSqlParameterSource[values.size()];
        for (int i = 0; i < values.size(); i++) {
            MemoryValue value = values.get(i);
            int memoryId = ensureMemory(deviceId, value.getName());
            batch[i] = new MapSqlParameterSource()
                    .addValue("memoryId", memoryId)
                    .addValue("value", value.getValue())
                    .addValue("status", true)
                    .addValue("updatedAt", Timestamp.valueOf(value.getUpdatedAt()));
        }
        namedTemplate.batchUpdate(SQL_INSERT_VALUE, batch);
        namedTemplate.batchUpdate(SQL_UPSERT_CURRENT, batch);
    }

    @Override
    public int pruneHistoryOlderThanDays(int days) {
        if (days <= 0) {
            return 0;
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("days", days);
        return namedTemplate.update(SQL_DELETE_HISTORY_OLDER_THAN_DAYS, params);
    }

    private MemoryValue mapRow(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        int value = rs.getInt("value");
        Timestamp ts = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = ts == null ? LocalDateTime.now() : ts.toLocalDateTime();
        return new MemoryValue(name, value, updatedAt);
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
        if (id == null) {
            throw new IllegalStateException("Device id was null for mnemonic " + mnemonic);
        }
        cachedDeviceId = id;
        cachedMnemonic = mnemonic;
        memoryIdCache.clear();
        return id.intValue();
    }

    private int ensureMemory(int deviceId, String name) {
        Integer cached = memoryIdCache.get(name);
        if (cached != null) {
            return cached;
        }
        int address = extractAddress(name);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("deviceId", deviceId)
                .addValue("name", name)
                .addValue("address", address);
        Integer id = namedTemplate.queryForObject(SQL_UPSERT_MEMORY, params, Integer.class);
        if (id == null) {
            throw new IllegalStateException("Memory id was null for name " + name);
        }
        memoryIdCache.put(name, id);
        return id.intValue();
    }

    private int extractAddress(String memoryName) {
        if (memoryName == null) {
            return 0;
        }
        if (memoryName.startsWith("DM_")) {
            try {
                return Integer.parseInt(memoryName.substring(3));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        if (memoryName.startsWith("RR_")) {
            int dot = memoryName.indexOf('.');
            if (dot > 3) {
                try {
                    return Integer.parseInt(memoryName.substring(3, dot));
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
