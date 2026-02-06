package org.ctrl.db.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DmValue;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class JdbcDmValueRepository implements DmValueRepository {

    private static final String SQL_UPSERT =
            "INSERT INTO dm_values (address, value, updated_at) " +
            "VALUES (:address, :value, :updatedAt) " +
            "ON CONFLICT (address) DO UPDATE " +
            "SET value = EXCLUDED.value, updated_at = EXCLUDED.updated_at";

    private static final String SQL_FIND_ONE =
            "SELECT address, value, updated_at FROM dm_values WHERE address = ?";

    private static final String SQL_FIND_RANGE =
            "SELECT address, value, updated_at FROM dm_values " +
            "WHERE address BETWEEN ? AND ? ORDER BY address";

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedTemplate;

    public JdbcDmValueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public Optional<DmValue> findByAddress(int address) {
        List<DmValue> rows = jdbcTemplate.query(SQL_FIND_ONE, mapper(), address);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(rows.get(0));
    }

    @Override
    public List<DmValue> findRange(int startAddress, int endAddress) {
        return jdbcTemplate.query(SQL_FIND_RANGE, mapper(), startAddress, endAddress);
    }

    @Override
    public void upsert(DmValue value) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("address", value.getAddress())
                .addValue("value", value.getValue())
                .addValue("updatedAt", Timestamp.from(value.getUpdatedAt()));
        namedTemplate.update(SQL_UPSERT, params);
    }

    @Override
    public void upsertBatch(List<DmValue> values) {
        MapSqlParameterSource[] batch = new MapSqlParameterSource[values.size()];
        for (int i = 0; i < values.size(); i++) {
            DmValue value = values.get(i);
            batch[i] = new MapSqlParameterSource()
                    .addValue("address", value.getAddress())
                    .addValue("value", value.getValue())
                    .addValue("updatedAt", Timestamp.from(value.getUpdatedAt()));
        }
        namedTemplate.batchUpdate(SQL_UPSERT, batch);
    }

    private RowMapper<DmValue> mapper() {
        return (ResultSet rs, int rowNum) -> mapRow(rs);
    }

    private DmValue mapRow(ResultSet rs) throws SQLException {
        int address = rs.getInt("address");
        int value = rs.getInt("value");
        Timestamp ts = rs.getTimestamp("updated_at");
        Instant updatedAt = ts == null ? Instant.now() : ts.toInstant();
        return new DmValue(address, value, updatedAt);
    }
}
