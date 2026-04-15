package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.Defeito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class DefeitoRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name, number FROM public.defeito ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, number FROM public.defeito WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.defeito (name, number) VALUES (?, ?) RETURNING id, name, number";
    private static final String SQL_UPDATE =
            "UPDATE public.defeito SET name = ?, number = ? WHERE id = ? RETURNING id, name, number";
    private static final String SQL_DELETE =
            "DELETE FROM public.defeito WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Defeito> rowMapper = this::mapRow;

    public DefeitoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Defeito> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<Defeito> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Defeito create(String name, Integer number) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), name, number);
    }

    public Optional<Defeito> update(long id, String name, Integer number) {
        return queryOptional(SQL_UPDATE, name, number, id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Defeito> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Defeito mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Defeito(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getObject("number") != null ? rs.getInt("number") : null
        );
    }
}
