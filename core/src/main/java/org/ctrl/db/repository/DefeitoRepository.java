package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.ctrl.db.model.Defeito;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DefeitoRepository {

    private final JdbcTemplate jdbcTemplate;

    public DefeitoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Defeito> findByNumber(int number) {
        String sql = "SELECT id, name, number FROM public.defeito WHERE number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRow, number));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Defeito mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Defeito(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("number")
        );
    }
}
