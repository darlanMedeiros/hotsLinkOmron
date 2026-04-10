package org.ctrl.db.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.Tag;
import org.ctrl.db.model.TagValue;
import org.ctrl.db.repository.TagRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;

public class TagService {

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

    private static final String SQL_FIND_MACHINE_BY_DEVICE =
            "SELECT id FROM public.machine WHERE device_id = :deviceId ORDER BY id LIMIT 1";

    private static final String SQL_FIND_CURRENT_BY_TAG =
            "SELECT t.name AS tag_name, " + MEMORY_KEY_EXPR + " AS memory_name, d.mnemonic AS device_mnemonic, " +
            "mvc.value, mvc.updated_at " +
            "FROM public.tag t " +
            "JOIN public.machine mc ON mc.id = t.machine_id " +
            "JOIN public.device d ON d.id = mc.device_id " +
            "JOIN public.memory m ON m.id = t.memory_id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND t.name = :tagName";

    private static final String SQL_FIND_CURRENT_BY_TAG_NAME =
            "SELECT t.name AS tag_name, " + MEMORY_KEY_EXPR + " AS memory_name, d.mnemonic AS device_mnemonic, " +
            "mvc.value, mvc.updated_at " +
            "FROM public.tag t " +
            "JOIN public.machine mc ON mc.id = t.machine_id " +
            "JOIN public.device d ON d.id = mc.device_id " +
            "JOIN public.memory m ON m.id = t.memory_id " +
            "LEFT JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE t.name = :tagName " +
            "ORDER BY d.mnemonic";

    private static final String SQL_FIND_CURRENT_BY_MEMORY_NAME =
            "SELECT mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND " + MEMORY_KEY_EXPR + " = :memoryName";

    private static final Map<String, org.ctrl.extras.Tag> TAG_CATALOG = buildTagCatalog();

    private final TagRepository repository;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final @NonNull RowMapper<TagValue> tagValueMapper = this::mapTagValue;
    private final Map<String, Integer> memoryIdCache = new HashMap<>();
    private Integer cachedDeviceId = null;
    private Integer cachedMachineId = null;
    private String cachedMnemonic = null;

    public TagService(TagRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public Tag createTag(DeviceInfo device, String tagName, String memoryName) {
        Objects.requireNonNull(device, "device");
        int deviceId = ensureDevice(device.getMnemonic(), device.getName(), device.getDescription());
        int machineId = ensureMachine(deviceId, device.getMnemonic());
        int memoryId = ensureMemory(deviceId, memoryName);
        return repository.create(tagName, machineId, memoryId);
    }

    public Tag getOrCreateTag(DeviceInfo device, String tagName, String memoryName) {
        Objects.requireNonNull(device, "device");
        int deviceId = ensureDevice(device.getMnemonic(), device.getName(), device.getDescription());
        int machineId = ensureMachine(deviceId, device.getMnemonic());
        int memoryId = ensureMemory(deviceId, memoryName);
        return repository.create(tagName, machineId, memoryId);
    }

    public Tag createDmTag(DeviceInfo device, String tagName, int address) {
        return createTag(device, tagName, DmValueService.formatDmName(address));
    }

    public Tag getOrCreateDmTag(DeviceInfo device, String tagName, int address) {
        return getOrCreateTag(device, tagName, DmValueService.formatDmName(address));
    }

    public Tag createRrTag(DeviceInfo device, String tagName, int address, int bit) {
        return createTag(device, tagName, RrValueService.formatRrName(address, bit));
    }

    public Tag getOrCreateRrTag(DeviceInfo device, String tagName, int address, int bit) {
        return getOrCreateTag(device, tagName, RrValueService.formatRrName(address, bit));
    }

    public Optional<TagValue> findCurrentByTag(String deviceMnemonic, String tagName) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", deviceMnemonic)
                .addValue("tagName", tagName);
        try {
            TagValue base = namedTemplate.queryForObject(SQL_FIND_CURRENT_BY_TAG, params, tagValueMapper);
            if (base == null) {
                return Optional.empty();
            }
            if (!isDmDwordTag(tagName, base.getMemoryName())) {
                return Optional.of(base);
            }
            return Optional.of(buildDwordValue(base));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<TagValue> findCurrentByTagName(String tagName) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("tagName", tagName);
        List<TagValue> rows = namedTemplate.query(SQL_FIND_CURRENT_BY_TAG_NAME, params, tagValueMapper);
        if (rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<TagValue> out = new ArrayList<>(rows.size());
        for (TagValue base : rows) {
            if (!isDmDwordTag(base.getTagName(), base.getMemoryName())) {
                out.add(base);
            } else {
                out.add(buildDwordValue(base));
            }
        }
        return out;
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
        cachedMachineId = null;
        cachedMnemonic = mnemonic;
        memoryIdCache.clear();
        return id.intValue();
    }

    private int ensureMachine(int deviceId, String mnemonic) {
        if (cachedMachineId != null && cachedDeviceId != null && cachedDeviceId.intValue() == deviceId) {
            return cachedMachineId.intValue();
        }

        MapSqlParameterSource params = new MapSqlParameterSource().addValue("deviceId", deviceId);
        Integer machineId = namedTemplate.query(SQL_FIND_MACHINE_BY_DEVICE, params, rs -> rs.next() ? rs.getInt("id") : null);
        if (machineId == null) {
            throw new IllegalStateException("No machine found for device mnemonic " + mnemonic);
        }
        cachedMachineId = machineId;
        return machineId.intValue();
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

    private TagValue mapTagValue(ResultSet rs, int rowNum) throws SQLException {
        String tagName = rs.getString("tag_name");
        String memoryName = rs.getString("memory_name");
        String deviceMnemonic = rs.getString("device_mnemonic");
        Integer value = rs.getInt("value");
        if (rs.wasNull()) {
            value = null;
        }
        java.sql.Timestamp ts = rs.getTimestamp("updated_at");
        LocalDateTime updatedAt = ts == null ? null : ts.toLocalDateTime();
        return new TagValue(tagName, memoryName, deviceMnemonic, value, updatedAt);
    }

    private TagValue buildDwordValue(TagValue base) {
        Integer lowWord = base.getValue();
        if (lowWord == null) {
            return base;
        }

        int startAddress = DmValueService.parseDmAddress(base.getMemoryName());
        String highMemoryName = DmValueService.formatDmName(startAddress + 1);
        Optional<MemoryCurrent> high = findCurrentByMemoryName(base.getDeviceMnemonic(), highMemoryName);
        if (high.isEmpty() || high.get().value == null) {
            return base;
        }

        int low = lowWord.intValue() & 0xFFFF;
        int highWord = high.get().value.intValue() & 0xFFFF;
        int dwordValue = (highWord << 16) | low;
        LocalDateTime updatedAt = mostRecent(base.getUpdatedAt(), high.get().updatedAt);
        return new TagValue(base.getTagName(), base.getMemoryName(), base.getDeviceMnemonic(), dwordValue, updatedAt);
    }

    private Optional<MemoryCurrent> findCurrentByMemoryName(String deviceMnemonic, String memoryName) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mnemonic", deviceMnemonic)
                .addValue("memoryName", memoryName);
        try {
            return Optional.ofNullable(namedTemplate.queryForObject(SQL_FIND_CURRENT_BY_MEMORY_NAME, params,
                    (rs, rowNum) -> {
                        Integer value = rs.getInt("value");
                        if (rs.wasNull()) {
                            value = null;
                        }
                        java.sql.Timestamp ts = rs.getTimestamp("updated_at");
                        LocalDateTime updatedAt = ts == null ? null : ts.toLocalDateTime();
                        return new MemoryCurrent(value, updatedAt);
                    }));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private boolean isDmDwordTag(String tagName, String memoryName) {
        if (memoryName == null || !memoryName.startsWith("DM_")) {
            return false;
        }
        org.ctrl.extras.Tag tag = TAG_CATALOG.get(tagName);
        return tag != null
                && tag.getArea() == org.ctrl.extras.Tag.Area.DM
                && tag.getDataType() == org.ctrl.extras.Tag.DataType.DWORD;
    }

    private static Map<String, org.ctrl.extras.Tag> buildTagCatalog() {
        Map<String, org.ctrl.extras.Tag> map = new HashMap<>();
        for (Field field : org.ctrl.extras.Tag.class.getDeclaredFields()) {
            if (field.getType() != org.ctrl.extras.Tag.class) {
                continue;
            }
            int mod = field.getModifiers();
            if (!Modifier.isStatic(mod)) {
                continue;
            }
            try {
                Object raw = field.get(null);
                if (raw instanceof org.ctrl.extras.Tag) {
                    org.ctrl.extras.Tag tag = (org.ctrl.extras.Tag) raw;
                    map.put(tag.getName(), tag);
                }
            } catch (IllegalAccessException ex) {
                // Ignore inaccessible fields.
            }
        }
        return Collections.unmodifiableMap(map);
    }

    private LocalDateTime mostRecent(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
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

    private static final class MemoryCurrent {
        private final Integer value;
        private final LocalDateTime updatedAt;

        private MemoryCurrent(Integer value, LocalDateTime updatedAt) {
            this.value = value;
            this.updatedAt = updatedAt;
        }
    }
}
