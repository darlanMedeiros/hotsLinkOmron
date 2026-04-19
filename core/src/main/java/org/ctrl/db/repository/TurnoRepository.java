package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.Turno;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TurnoRepository {

    private final JdbcTemplate jdbcTemplate;

    public TurnoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SQL_FIND_ALL =
            "SELECT id, name, hora_inicio::text AS hora_inicio, hora_final::text AS hora_final FROM public.turno ORDER BY id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, name, hora_inicio::text AS hora_inicio, hora_final::text AS hora_final FROM public.turno WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.turno (name, hora_inicio, hora_final) VALUES (?, ?::time, ?::time) RETURNING id";
    private static final String SQL_UPDATE =
            "UPDATE public.turno SET name = ?, hora_inicio = ?::time, hora_final = ?::time WHERE id = ?";
    private static final String SQL_DELETE =
            "DELETE FROM public.turno WHERE id = ?";

    public List<Turno> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, this::mapRow);
    }

    public Optional<Turno> findById(long id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(SQL_FIND_BY_ID, this::mapRow, id));
        } catch (org.springframework.dao.EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Long create(String name, LocalTime horaInicio, LocalTime horaFinal) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Long.class, name, horaInicio.toString(), horaFinal.toString());
    }

    public void update(long id, String name, LocalTime horaInicio, LocalTime horaFinal) {
        jdbcTemplate.update(SQL_UPDATE, name, horaInicio.toString(), horaFinal.toString(), id);
    }

    public boolean delete(long id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    public Optional<Turno> findTurnoByTime(LocalTime time) {
        return findAll().stream()
                .filter(t -> t.contains(time))
                .findFirst();
    }

    private Turno mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Turno(
                rs.getLong("id"),
                rs.getString("name"),
                LocalTime.parse(rs.getString("hora_inicio")),
                LocalTime.parse(rs.getString("hora_final"))
        );
    }
}
