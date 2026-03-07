package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.MiniFabrica;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MiniFabricaRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name, fabrica_id FROM public.mini_fabrica ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, fabrica_id FROM public.mini_fabrica WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.mini_fabrica (name, fabrica_id) VALUES (?, ?) RETURNING id, name, fabrica_id";
    private static final String SQL_UPDATE =
            "UPDATE public.mini_fabrica SET name = ?, fabrica_id = ? WHERE id = ? RETURNING id, name, fabrica_id";
    private static final String SQL_DELETE =
            "DELETE FROM public.mini_fabrica WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MiniFabrica> rowMapper = this::mapRow;

    public MiniFabricaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MiniFabrica> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, rowMapper);
    }

    public Optional<MiniFabrica> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public MiniFabrica create(String name, long fabricaId) {
        return jdbcTemplate.queryForObject(SQL_INSERT, rowMapper, name, fabricaId);
    }

    public Optional<MiniFabrica> update(long id, String name, long fabricaId) {
        return queryOptional(SQL_UPDATE, name, fabricaId, id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<MiniFabrica> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private MiniFabrica mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MiniFabrica(rs.getLong("id"), rs.getString("name"), rs.getLong("fabrica_id"));
    }
}
