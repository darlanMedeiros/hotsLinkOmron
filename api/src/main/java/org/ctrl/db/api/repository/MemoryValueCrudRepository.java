package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.MemoryValueCrud;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryValueCrudRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, memory_id, value, status, updated_at FROM public.memory_value ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, memory_id, value, status, updated_at FROM public.memory_value WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.memory_value (memory_id, value, status, updated_at) " +
            "VALUES (?, ?, ?, COALESCE(?, NOW())) " +
            "RETURNING id, memory_id, value, status, updated_at";
    private static final String SQL_UPDATE =
            "UPDATE public.memory_value " +
            "SET memory_id = ?, value = ?, status = ?, updated_at = COALESCE(?, updated_at) " +
            "WHERE id = ? " +
            "RETURNING id, memory_id, value, status, updated_at";
    private static final String SQL_DELETE =
            "DELETE FROM public.memory_value WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MemoryValueCrud> rowMapper = this::mapRow;

    public MemoryValueCrudRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MemoryValueCrud> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<MemoryValueCrud> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public MemoryValueCrud create(int memoryId, int value, boolean status, LocalDateTime updatedAt) {
        Timestamp ts = updatedAt == null ? null : Timestamp.valueOf(updatedAt);
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), memoryId, value, status, ts);
    }

    public Optional<MemoryValueCrud> update(int id, int memoryId, int value, boolean status, LocalDateTime updatedAt) {
        Timestamp ts = updatedAt == null ? null : Timestamp.valueOf(updatedAt);
        return queryOptional(SQL_UPDATE, memoryId, value, status, ts, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<MemoryValueCrud> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private MemoryValueCrud mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp ts = rs.getTimestamp("updated_at");
        return new MemoryValueCrud(
                rs.getInt("id"),
                rs.getInt("memory_id"),
                rs.getInt("value"),
                rs.getBoolean("status"),
                ts == null ? null : ts.toLocalDateTime());
    }
}

