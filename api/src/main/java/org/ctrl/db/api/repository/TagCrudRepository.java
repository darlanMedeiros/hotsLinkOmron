package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.TagCrud;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TagCrudRepository {

    private static final String SQL_FIND_ALL =
            "SELECT t.id, t.name, t.device_id, t.memory_id " +
                    "FROM public.tag t " +
                    "JOIN public.device d ON d.id = t.device_id " +
                    "JOIN public.memory m ON m.id = t.memory_id " +
                    "ORDER BY d.mnemonic, t.device_id, m.name, t.name, t.id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, device_id, memory_id FROM public.tag WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.tag (name, device_id, memory_id) VALUES (?, ?, ?) RETURNING id, name, device_id, memory_id";
    private static final String SQL_UPDATE =
            "UPDATE public.tag SET name = ?, device_id = ?, memory_id = ? WHERE id = ? RETURNING id, name, device_id, memory_id";
    private static final String SQL_DELETE =
            "DELETE FROM public.tag WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<TagCrud> rowMapper = this::mapRow;

    public TagCrudRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TagCrud> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, rowMapper);
    }

    public Optional<TagCrud> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public TagCrud create(String name, int deviceId, int memoryId) {
        return jdbcTemplate.queryForObject(SQL_INSERT, rowMapper, name, deviceId, memoryId);
    }

    public Optional<TagCrud> update(int id, String name, int deviceId, int memoryId) {
        return queryOptional(SQL_UPDATE, name, deviceId, memoryId, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<TagCrud> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private TagCrud mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TagCrud(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getInt("device_id"),
                rs.getInt("memory_id"));
    }
}
