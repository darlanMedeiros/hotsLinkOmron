package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.model.Tag;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcTagRepository implements TagRepository {

    private static final String SQL_INSERT =
            "INSERT INTO public.tag (name, device_id, memory_id) " +
            "VALUES (:name, :deviceId, :memoryId) " +
            "RETURNING id, name, device_id, memory_id";

    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, device_id, memory_id " +
            "FROM public.tag WHERE id = ?";

    private static final String SQL_FIND_BY_DEVICE_NAME =
            "SELECT id, name, device_id, memory_id " +
            "FROM public.tag WHERE device_id = ? AND name = ?";

    private static final String SQL_FIND_BY_MEMORY_ID =
            "SELECT id, name, device_id, memory_id " +
            "FROM public.tag WHERE memory_id = ?";

    private static final String SQL_FIND_BY_DEVICE_ID =
            "SELECT id, name, device_id, memory_id " +
            "FROM public.tag WHERE device_id = ? " +
            "ORDER BY name";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;
    private final RowMapper<Tag> rowMapper = this::mapRow;

    public JdbcTagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.namedTemplate = new NamedParameterJdbcTemplate(this.jdbcTemplate);
    }

    @Override
    public Tag create(String name, int deviceId, int memoryId) {
        Optional<Tag> existing = findByMemoryId(memoryId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Memory id " + memoryId + " already has a tag");
        }
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("deviceId", deviceId)
                .addValue("memoryId", memoryId);
        return namedTemplate.queryForObject(SQL_INSERT, params, rowMapper);
    }

    @Override
    public Optional<Tag> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    @Override
    public Optional<Tag> findByDeviceAndName(int deviceId, String name) {
        return queryOptional(SQL_FIND_BY_DEVICE_NAME, deviceId, name);
    }

    @Override
    public Optional<Tag> findByMemoryId(int memoryId) {
        return queryOptional(SQL_FIND_BY_MEMORY_ID, memoryId);
    }

    @Override
    public List<Tag> findByDeviceId(int deviceId) {
        return jdbcTemplate.query(SQL_FIND_BY_DEVICE_ID, rowMapper, deviceId);
    }

    private Optional<Tag> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Tag mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer id = rs.getInt("id");
        if (rs.wasNull()) {
            id = null;
        }
        String name = rs.getString("name");
        int deviceId = rs.getInt("device_id");
        int memoryId = rs.getInt("memory_id");
        return new Tag(id, name, deviceId, memoryId);
    }
}
