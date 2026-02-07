package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.ctrl.db.model.RrValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcRrValueRepository implements RrValueRepository {

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

    private static final String SQL_FIND_ONE_CURRENT =
            "SELECT m.name, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = ? AND m.name = ? " +
            "LIMIT 1";

    private static final String SQL_FIND_RANGE_CURRENT =
            "SELECT m.name, mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND m.name IN (:names) " +
            "ORDER BY m.name";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final Map<String, Integer> memoryIdCache = new HashMap<>();
    private Integer cachedDeviceId = null;
    private String cachedMnemonic = null;

    public JdbcRrValueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Optional<RrValue> findCurrentByAddressBit(String deviceMnemonic, int address, int bit) {
        String name = formatRrName(address, bit);
        List<RrValue> rows = jdbcTemplate.query(SQL_FIND_ONE_CURRENT, mapper(), deviceMnemonic, name);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Override
    public List<RrValue> findRangeCurrent(String deviceMnemonic, int address, int startBit, int endBit) {
        List<String> names = buildRrNames(address, startBit, endBit);
        if (names.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", deviceMnemonic)
                .addValue("names", names);
        return namedTemplate.query(SQL_FIND_RANGE_CURRENT, params, mapper());
    }

    @Override
    public void upsert(String deviceMnemonic, String deviceName, String deviceDescription, RrValue value) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        int memoryId = ensureMemory(deviceId, formatRrName(value.getAddress(), value.getBit()));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("memoryId", memoryId)
                .addValue("value", value.isValue() ? 1 : 0)
                .addValue("status", true)
                .addValue("updatedAt", Timestamp.from(value.getUpdatedAt()));
        namedTemplate.update(SQL_INSERT_VALUE, params);
        namedTemplate.update(SQL_UPSERT_CURRENT, params);
    }

    @Override
    public void upsertBatch(String deviceMnemonic, String deviceName, String deviceDescription, List<RrValue> values) {
        int deviceId = ensureDevice(deviceMnemonic, deviceName, deviceDescription);
        MapSqlParameterSource[] batch = new MapSqlParameterSource[values.size()];
        for (int i = 0; i < values.size(); i++) {
            RrValue value = values.get(i);
            int memoryId = ensureMemory(deviceId, formatRrName(value.getAddress(), value.getBit()));
            batch[i] = new MapSqlParameterSource()
                    .addValue("memoryId", memoryId)
                    .addValue("value", value.isValue() ? 1 : 0)
                    .addValue("status", true)
                    .addValue("updatedAt", Timestamp.from(value.getUpdatedAt()));
        }
        namedTemplate.batchUpdate(SQL_INSERT_VALUE, batch);
        namedTemplate.batchUpdate(SQL_UPSERT_CURRENT, batch);
    }

    private RowMapper<RrValue> mapper() {
        return (ResultSet rs, int rowNum) -> mapRow(rs);
    }

    private RrValue mapRow(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        Parsed parsed = parseRrName(name);
        int value = rs.getInt("value");
        Timestamp ts = rs.getTimestamp("updated_at");
        Instant updatedAt = ts == null ? Instant.now() : ts.toInstant();
        return new RrValue(parsed.address, parsed.bit, value != 0, updatedAt);
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

    private String formatRrName(int address, int bit) {
        return String.format("RR_%04d.%02d", address, bit);
    }

    private Parsed parseRrName(String name) {
        if (name == null || !name.startsWith("RR_")) {
            return new Parsed(0, 0);
        }
        String rest = name.substring(3);
        int dot = rest.indexOf('.');
        if (dot <= 0 || dot == rest.length() - 1) {
            return new Parsed(0, 0);
        }
        try {
            int address = Integer.parseInt(rest.substring(0, dot));
            int bit = Integer.parseInt(rest.substring(dot + 1));
            return new Parsed(address, bit);
        } catch (NumberFormatException ex) {
            return new Parsed(0, 0);
        }
    }

    private List<String> buildRrNames(int address, int startBit, int endBit) {
        int s = Math.max(0, startBit);
        int e = Math.min(15, endBit);
        int size = Math.max(0, e - s + 1);
        java.util.ArrayList<String> names = new java.util.ArrayList<>(size);
        for (int bit = s; bit <= e; bit++) {
            names.add(formatRrName(address, bit));
        }
        return names;
    }

    private static final class Parsed {
        private final int address;
        private final int bit;

        private Parsed(int address, int bit) {
            this.address = address;
            this.bit = bit;
        }
    }
}
