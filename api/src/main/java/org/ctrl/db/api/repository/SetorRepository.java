package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Setor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SetorRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name, mini_fabrica_id FROM public.setor ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, mini_fabrica_id FROM public.setor WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.setor (name, mini_fabrica_id) VALUES (?, ?) RETURNING id, name, mini_fabrica_id";
    private static final String SQL_UPDATE =
            "UPDATE public.setor SET name = ?, mini_fabrica_id = ? WHERE id = ? RETURNING id, name, mini_fabrica_id";
    private static final String SQL_DELETE =
            "DELETE FROM public.setor WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Setor> rowMapper = this::mapRow;

    public SetorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Setor> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, rowMapper);
    }

    public Optional<Setor> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Setor create(String name, long miniFabricaId) {
        return jdbcTemplate.queryForObject(SQL_INSERT, rowMapper, name, miniFabricaId);
    }

    public Optional<Setor> update(long id, String name, long miniFabricaId) {
        return queryOptional(SQL_UPDATE, name, miniFabricaId, id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Setor> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Setor mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Setor(rs.getLong("id"), rs.getString("name"), rs.getLong("mini_fabrica_id"));
    }
}
