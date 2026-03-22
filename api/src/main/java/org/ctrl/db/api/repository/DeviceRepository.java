package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.Device;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DeviceRepository {

    private static final String SQL_FIND_ALL =
            "SELECT d.id, d.mnemonic, d.name, d.description, d.no_id AS node_id " +
                    "FROM public.device d " +
                    "ORDER BY d.id";
    private static final String SQL_FIND_BY_ID =
            "SELECT d.id, d.mnemonic, d.name, d.description, d.no_id AS node_id " +
                    "FROM public.device d " +
                    "WHERE d.id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.device (mnemonic, name, description, no_id) VALUES (?, ?, ?, ?) " +
                    "RETURNING id, mnemonic, name, description, no_id AS node_id";
    private static final String SQL_UPDATE =
            "UPDATE public.device SET mnemonic = ?, name = ?, description = ?, no_id = ? WHERE id = ? " +
                    "RETURNING id, mnemonic, name, description, no_id AS node_id";
    private static final String SQL_DELETE =
            "DELETE FROM public.device WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Device> rowMapper = this::mapRow;

    public DeviceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Device> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<Device> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Device create(String mnemonic, String name, String description, Integer nodeId) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), mnemonic, name, description, nodeId);
    }

    public Optional<Device> update(int id, String mnemonic, String name, String description, Integer nodeId) {
        return queryOptional(SQL_UPDATE, mnemonic, name, description, nodeId, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Device> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Device mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Device(
                rs.getInt("id"),
                rs.getString("mnemonic"),
                rs.getString("name"),
                rs.getString("description"),
                (Integer) rs.getObject("node_id"));
    }

}

