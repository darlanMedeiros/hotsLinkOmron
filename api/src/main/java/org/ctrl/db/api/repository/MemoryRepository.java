package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Memory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, device_id, name FROM public.memory ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, device_id, name FROM public.memory WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.memory (device_id, name) VALUES (?, ?) RETURNING id, device_id, name";
    private static final String SQL_UPDATE =
            "UPDATE public.memory SET device_id = ?, name = ? WHERE id = ? RETURNING id, device_id, name";
    private static final String SQL_DELETE =
            "DELETE FROM public.memory WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Memory> rowMapper = this::mapRow;

    public MemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Memory> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, rowMapper);
    }

    public Optional<Memory> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Memory create(int deviceId, String name) {
        return jdbcTemplate.queryForObject(SQL_INSERT, rowMapper, deviceId, name);
    }

    public Optional<Memory> update(int id, int deviceId, String name) {
        return queryOptional(SQL_UPDATE, deviceId, name, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Memory> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Memory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Memory(rs.getInt("id"), rs.getInt("device_id"), rs.getString("name"));
    }
}
