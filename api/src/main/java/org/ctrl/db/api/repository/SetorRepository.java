package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.Setor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class SetorRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name FROM public.setor ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name FROM public.setor WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.setor (name) VALUES (?) RETURNING id, name";
    private static final String SQL_UPDATE =
            "UPDATE public.setor SET name = ? WHERE id = ? RETURNING id, name";
    private static final String SQL_DELETE =
            "DELETE FROM public.setor WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Setor> rowMapper = this::mapRow;

    public SetorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Setor> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<Setor> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Setor create(String name) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), name);
    }

    public Optional<Setor> update(long id, String name) {
        return queryOptional(SQL_UPDATE, name, id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Setor> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Setor mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Setor(rs.getLong("id"), rs.getString("name"));
    }
}
