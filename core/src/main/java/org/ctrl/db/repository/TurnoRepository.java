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

    public List<Turno> findAll() {
        String sql = "SELECT id, name, hora_inicio::text, hora_final::text FROM public.turno ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRow);
    }

    public Optional<Turno> findTurnoByTime(LocalTime time) {
        // Buscamos todos os turnos e filtramos em memória para lidar com lógica de cruzamento de meia-noite
        // que é complexa de fazer apenas em SQL puro de forma portável across DBs, 
        // embora no Postgres seja possível. Usar a lógica do POJO garante consistência.
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
