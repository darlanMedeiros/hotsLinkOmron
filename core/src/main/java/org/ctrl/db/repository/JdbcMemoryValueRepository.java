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

    private static final String MEMORY_KEY_EXPR =
            "CONCAT(m.name, '_', LPAD(m.address::text, 4, '0'), " +
            "CASE WHEN m.bit >= 0 THEN CONCAT('.', LPAD(m.bit::text, 2, '0')) ELSE '' END)";

    private static final String SQL_UPSERT_DEVICE =
            "INSERT INTO public.device (mnemonic, name, description) " +
            "VALUES (:mnemonic, :name, :description) " +
            "ON CONFLICT (mnemonic) DO UPDATE " +
            "SET name = EXCLUDED.name, description = EXCLUDED.description " +
            "RETURNING id";

    private static final String SQL_UPSERT_MEMORY =
            "INSERT INTO public.memory (device_id, name, address, bit) " +
            "VALUES (:deviceId, :name, :address, :bit) " +
            "ON CONFLICT (device_id, name, address, bit) DO UPDATE " +
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

    private static final String SQL_FIND_LATEST_BY_NAME =
            "SELECT " + MEMORY_KEY_EXPR + " AS memory_key, mv.value, mv.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value mv ON mv.memory_id = m.id " +
            "WHERE d.mnemonic = ? AND " + MEMORY_KEY_EXPR + " = ? " +
            "ORDER BY mv.updated_at DESC " +
            "LIMIT 1";

    private static final String SQL_FIND_CURRENT_BY_NAME =
            "SELECT " + MEMORY_KEY_EXPR + " AS memory_key, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = ? AND " + MEMORY_KEY_EXPR + " = ? " +
            "LIMIT 1";

    private static final String SQL_FIND_RANGE_LATEST =
            "SELECT DISTINCT ON (m.id) " + MEMORY_KEY_EXPR + " AS memory_key, mv.value, mv.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value mv ON mv.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND " + MEMORY_KEY_EXPR + " IN (:names) " +
            "ORDER BY m.id, mv.updated_at DESC";

    private static final String SQL_FIND_RANGE_CURRENT =
            "SELECT " + MEMORY_KEY_EXPR + " AS memory_key, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND " + MEMORY_KEY_EXPR + " IN (:names) " +
            "ORDER BY " + MEMORY_KEY_EXPR;

    private static final String SQL_FIND_LATEST_CURRENT =
            "SELECT " + MEMORY_KEY_EXPR + " AS memory_key, mvc.value, mvc.updated_at " +
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
    public void upsertCurrent(String deviceMnemonic, String deviceName, String deviceDescription, MemoryValue value) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        int memoryId = ensureMemory(deviceId, value.getName());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memoryId", memoryId)
                .addValue("value", value.getValue())
                .addValue("status", true)
                .addValue("updatedAt", Timestamp.valueOf(value.getUpdatedAt()));
        namedTemplate.update(SQL_UPSERT_CURRENT, params);
    }

    @Override
    public void upsertBatchCurrent(String deviceMnemonic, String deviceName, String deviceDescription,
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
        String name = rs.getString("memory_key");
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
        ParsedMemoryKey parsed = parseMemoryKey(name);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("deviceId", deviceId)
                .addValue("name", parsed.area)
                .addValue("address", parsed.address)
                .addValue("bit", parsed.bit);
        Integer id = namedTemplate.queryForObject(SQL_UPSERT_MEMORY, params, Integer.class);
        if (id == null) {
            throw new IllegalStateException("Memory id was null for name " + name);
        }
        memoryIdCache.put(name, id);
        return id.intValue();
    }

    private ParsedMemoryKey parseMemoryKey(String memoryName) {
        if (memoryName == null) {
            return new ParsedMemoryKey("DM", 0, -1);
        }
        String normalized = memoryName.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return new ParsedMemoryKey("DM", 0, -1);
        }

        int underscore = normalized.indexOf('_');
        if (underscore < 0) {
            return new ParsedMemoryKey(normalized, 0, -1);
        }

        String area = normalized.substring(0, underscore).trim();
        String rest = normalized.substring(underscore + 1).trim();
        if (rest.isEmpty()) {
            return new ParsedMemoryKey(area, 0, -1);
        }

        int dot = rest.indexOf('.');
        try {
            if (dot < 0) {
                return new ParsedMemoryKey(area, Integer.parseInt(rest), -1);
            }
            int address = Integer.parseInt(rest.substring(0, dot));
            int bit = Integer.parseInt(rest.substring(dot + 1));
            return new ParsedMemoryKey(area, address, bit);
        } catch (NumberFormatException ex) {
            return new ParsedMemoryKey(area, 0, -1);
        }
    }

    private static final class ParsedMemoryKey {
        private final String area;
        private final int address;
        private final int bit;

        private ParsedMemoryKey(String area, int address, int bit) {
            this.area = area == null || area.trim().isEmpty() ? "DM" : area.trim();
            this.address = Math.max(0, address);
            this.bit = Math.max(-1, Math.min(15, bit));
        }
    }
}
