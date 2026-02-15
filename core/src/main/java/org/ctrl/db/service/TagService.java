package org.ctrl.db.service;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
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

    private static final String SQL_FIND_CURRENT_BY_TAG =
            "SELECT t.name AS tag_name, m.name AS memory_name, d.mnemonic AS device_mnemonic, " +
            "mvc.value, mvc.updated_at " +
            "FROM public.tag t " +
            "JOIN public.device d ON d.id = t.device_id " +
            "JOIN public.memory m ON m.id = t.memory_id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND t.name = :tagName";

    private static final String SQL_FIND_CURRENT_BY_MEMORY_NAME =
            "SELECT mvc.value, mvc.updated_at " +
            "FROM public.device d " +
            "JOIN public.memory m ON m.device_id = d.id " +
            "JOIN public.memory_value_current mvc ON mvc.memory_id = m.id " +
            "WHERE d.mnemonic = :mnemonic AND m.name = :memoryName";

    private static final Map<String, org.ctrl.extras.Tag> TAG_CATALOG = buildTagCatalog();

    private final TagRepository repository;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final @NonNull RowMapper<TagValue> tagValueMapper = this::mapTagValue;
    private final Map<String, Integer> memoryIdCache = new HashMap<>();
    private Integer cachedDeviceId = null;
    private String cachedMnemonic = null;

    public TagService(TagRepository repository, JdbcTemplate jdbcTemplate) {
        this.repository = Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    public Tag createTag(DeviceInfo device, String tagName, String memoryName) {
        Objects.requireNonNull(device, "device");
        int deviceId = ensureDevice(device.getMnemonic(), device.getName(), device.getDescription());
        int memoryId = ensureMemory(deviceId, memoryName);
        return repository.create(tagName, deviceId, memoryId);
    }

    public Tag getOrCreateTag(DeviceInfo device, String tagName, String memoryName) {
        Objects.requireNonNull(device, "device");
        int deviceId = ensureDevice(device.getMnemonic(), device.getName(), device.getDescription());
        Optional<Tag> existing = repository.findByDeviceAndName(deviceId, tagName);
        if (existing.isPresent()) {
            return existing.get();
        }
        int memoryId = ensureMemory(deviceId, memoryName);
        return repository.create(tagName, deviceId, memoryId);
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
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("deviceId", deviceId)
                .addValue("name", name);
        Integer id = namedTemplate.queryForObject(SQL_UPSERT_MEMORY, params, Integer.class);
        if (id == null) {
            throw new IllegalStateException("Memory id was null for name " + name);
        }
        memoryIdCache.put(name, id);
        return id.intValue();
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

    private static final class MemoryCurrent {
        private final Integer value;
        private final LocalDateTime updatedAt;

        private MemoryCurrent(Integer value, LocalDateTime updatedAt) {
            this.value = value;
            this.updatedAt = updatedAt;
        }
    }
}
