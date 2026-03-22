package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.Machine;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MachineRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name, device_id, setor_id FROM public.machine ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, device_id, setor_id FROM public.machine WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.machine (name, device_id, setor_id) VALUES (?, ?, ?) RETURNING id, name, device_id, setor_id";
    private static final String SQL_UPDATE =
            "UPDATE public.machine SET name = ?, device_id = ?, setor_id = ? WHERE id = ? RETURNING id, name, device_id, setor_id";
    private static final String SQL_DELETE =
            "DELETE FROM public.machine WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Machine> rowMapper = this::mapRow;

    public MachineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Machine> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<Machine> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Machine create(String name, int deviceId, long setorId) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), name, deviceId, setorId);
    }

    public Optional<Machine> update(long id, String name, int deviceId, long setorId) {
        return queryOptional(SQL_UPDATE, name, deviceId, setorId, id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Machine> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Machine mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Machine(rs.getLong("id"), rs.getString("name"), rs.getInt("device_id"), rs.getLong("setor_id"));
    }
}

