package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.TagCrud;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TagCrudRepository {

    private static final String SQL_FIND_ALL =
            "SELECT t.id, t.name, t.machine_id, t.memory_id, t.persist_history " +
                    "FROM public.tag t " +
                    "JOIN public.machine mc ON mc.id = t.machine_id " +
                    "JOIN public.device d ON d.id = mc.device_id " +
                    "JOIN public.memory m ON m.id = t.memory_id " +
                    "ORDER BY d.mnemonic, mc.id, m.name, t.name, t.id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, machine_id, memory_id, persist_history FROM public.tag WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.tag (name, machine_id, memory_id, persist_history) VALUES (?, ?, ?, ?) RETURNING id, name, machine_id, memory_id, persist_history";
    private static final String SQL_UPDATE =
            "UPDATE public.tag SET name = ?, machine_id = ?, memory_id = ?, persist_history = ? WHERE id = ? RETURNING id, name, machine_id, memory_id, persist_history";
    private static final String SQL_DELETE =
            "DELETE FROM public.tag WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<TagCrud> rowMapper = this::mapRow;

    public TagCrudRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TagCrud> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<TagCrud> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public TagCrud create(String name, long machineId, int memoryId, boolean persistHistory) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), name, machineId, memoryId, persistHistory);
    }

    public Optional<TagCrud> update(int id, String name, long machineId, int memoryId, boolean persistHistory) {
        return queryOptional(SQL_UPDATE, name, machineId, memoryId, persistHistory, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<TagCrud> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private TagCrud mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TagCrud(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getLong("machine_id"),
                rs.getInt("memory_id"),
                rs.getBoolean("persist_history"));
    }
}
