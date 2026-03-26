package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.Turno;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TurnoRepository {

    private static final String SQL_FIND_ALL =
            "SELECT id, name, hora_inicio::text AS hora_inicio, hora_final::text AS hora_final FROM public.turno ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, hora_inicio::text AS hora_inicio, hora_final::text AS hora_final FROM public.turno WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.turno (name, hora_inicio, hora_final) VALUES (?, ?::time, ?::time) RETURNING id, name, hora_inicio::text AS hora_inicio, hora_final::text AS hora_final";
    private static final String SQL_UPDATE =
            "UPDATE public.turno SET name = ?, hora_inicio = ?::time, hora_final = ?::time WHERE id = ? RETURNING id, name, hora_inicio::text AS hora_inicio, hora_final::text AS hora_final";
    private static final String SQL_DELETE =
            "DELETE FROM public.turno WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Turno> rowMapper = this::mapRow;

    public TurnoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Turno> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<Turno> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Turno create(String name, String horaInicio, String horaFinal) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), name, horaInicio, horaFinal);
    }

    public Optional<Turno> update(long id, String name, String horaInicio, String horaFinal) {
        return queryOptional(SQL_UPDATE, name, horaInicio, horaFinal, id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Turno> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Turno mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Turno(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("hora_inicio"),
                rs.getString("hora_final"));
    }
}
