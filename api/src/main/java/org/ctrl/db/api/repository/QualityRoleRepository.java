package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.QualityRoleOption;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class QualityRoleRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name, description, active FROM public.quality_role ORDER BY name";
    private static final String SQL_FIND_ACTIVE =
            "SELECT id, name, description, active FROM public.quality_role WHERE active = true ORDER BY name";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, description, active FROM public.quality_role WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.quality_role (name, description, active) VALUES (?, ?, ?) RETURNING id, name, description, active";
    private static final String SQL_UPDATE =
            "UPDATE public.quality_role SET name = ?, description = ?, active = ? WHERE id = ? RETURNING id, name, description, active";
    private static final String SQL_DELETE =
            "DELETE FROM public.quality_role WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<QualityRoleOption> rowMapper = this::mapRow;

    public QualityRoleRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<QualityRoleOption> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public List<QualityRoleOption> findActive() {
        return jdbcTemplate.query(SQL_FIND_ACTIVE, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<QualityRoleOption> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public QualityRoleOption create(String name, String description, boolean active) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), name, description,
                active);
    }

    public Optional<QualityRoleOption> update(int id, String name, String description, boolean active) {
        return queryOptional(SQL_UPDATE, name, description, active, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<QualityRoleOption> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"),
                    Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private QualityRoleOption mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new QualityRoleOption(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("active"));
    }
}

