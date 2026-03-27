package org.ctrl.db.api.repository;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.ctrl.db.api.model.MiniFabrica;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MiniFabricaRepository {

    private static final String SQL_FIND_ALL =
            "SELECT mf.id, mf.name, mf.fabrica_id, "
                    + "COALESCE(array_agg(mfs.setor_id ORDER BY mfs.setor_id) "
                    + "FILTER (WHERE mfs.setor_id IS NOT NULL), '{}'::bigint[]) AS setor_ids "
                    + "FROM public.mini_fabrica mf "
                    + "LEFT JOIN public.mini_fabrica_setor mfs ON mfs.mini_fabrica_id = mf.id "
                    + "GROUP BY mf.id, mf.name, mf.fabrica_id "
                    + "ORDER BY mf.id";
    private static final String SQL_FIND_BY_ID =
            "SELECT mf.id, mf.name, mf.fabrica_id, "
                    + "COALESCE(array_agg(mfs.setor_id ORDER BY mfs.setor_id) "
                    + "FILTER (WHERE mfs.setor_id IS NOT NULL), '{}'::bigint[]) AS setor_ids "
                    + "FROM public.mini_fabrica mf "
                    + "LEFT JOIN public.mini_fabrica_setor mfs ON mfs.mini_fabrica_id = mf.id "
                    + "WHERE mf.id = ? "
                    + "GROUP BY mf.id, mf.name, mf.fabrica_id";
    private static final String SQL_INSERT =
            "INSERT INTO public.mini_fabrica (name, fabrica_id) VALUES (?, ?) RETURNING id";
    private static final String SQL_UPDATE =
            "UPDATE public.mini_fabrica SET name = ?, fabrica_id = ? WHERE id = ?";
    private static final String SQL_DELETE =
            "DELETE FROM public.mini_fabrica WHERE id = ?";
    private static final String SQL_DELETE_REL =
            "DELETE FROM public.mini_fabrica_setor WHERE mini_fabrica_id = ?";
    private static final String SQL_INSERT_REL =
            "INSERT INTO public.mini_fabrica_setor (mini_fabrica_id, setor_id) VALUES (?, ?)";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MiniFabrica> rowMapper = this::mapRow;

    public MiniFabricaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MiniFabrica> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<MiniFabrica> findById(long id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    @Transactional
    public MiniFabrica create(String name, long fabricaId, List<Long> setorIds) {
        Long createdId = jdbcTemplate.queryForObject(SQL_INSERT, Long.class, name, fabricaId);
        if (createdId == null) {
            throw new IllegalStateException("failed to create mini_fabrica");
        }
        syncSetores(createdId, setorIds);
        return findById(createdId).orElseThrow(() -> new IllegalStateException("created mini_fabrica not found"));
    }

    @Transactional
    public Optional<MiniFabrica> update(long id, String name, long fabricaId, List<Long> setorIds) {
        int updatedRows = jdbcTemplate.update(SQL_UPDATE, name, fabricaId, id);
        if (updatedRows == 0) {
            return Optional.empty();
        }
        syncSetores(id, setorIds);
        return findById(id);
    }

    @Transactional
    public boolean delete(long id) {
        jdbcTemplate.update(SQL_DELETE_REL, id);
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private void syncSetores(long miniFabricaId, List<Long> setorIds) {
        jdbcTemplate.update(SQL_DELETE_REL, miniFabricaId);
        if (setorIds == null || setorIds.isEmpty()) {
            return;
        }
        for (Long setorId : setorIds) {
            jdbcTemplate.update(SQL_INSERT_REL, miniFabricaId, setorId);
        }
    }

    private Optional<MiniFabrica> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"), Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private MiniFabrica mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new MiniFabrica(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("fabrica_id"),
                readSetorIds(rs.getArray("setor_ids")));
    }

    private List<Long> readSetorIds(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return Collections.emptyList();
        }
        Object arrayObj = sqlArray.getArray();
        sqlArray.free();
        if (!(arrayObj instanceof Object[] raw)) {
            return Collections.emptyList();
        }

        List<Long> ids = new ArrayList<>(raw.length);
        for (Object item : raw) {
            if (item instanceof Number number) {
                ids.add(number.longValue());
            }
        }
        return ids;
    }
}
