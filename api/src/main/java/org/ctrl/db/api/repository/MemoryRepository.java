package org.ctrl.db.api.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.ctrl.db.api.dto.MemoryValueByDeviceDTO;
import org.ctrl.db.api.model.Memory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRepository {

    private static final String MEMORY_KEY_SQL =
            "CONCAT(m.name, '_', LPAD(m.address::text, 4, '0'), " +
            "CASE WHEN m.bit >= 0 THEN CONCAT('.', LPAD(m.bit::text, 2, '0')) ELSE '' END)";

    private static final String SQL_FIND_ALL = "SELECT m.id, m.device_id, m.name AS area, m.address, m.bit " +
            "FROM public.memory m " +
            "JOIN public.device d ON d.id = m.device_id " +
            "ORDER BY d.mnemonic, m.device_id, m.name, m.address, m.bit, m.id";
    private static final String SQL_FIND_BY_ID =
            "SELECT id, device_id, name AS area, address, bit FROM public.memory WHERE id = ?";
    private static final String SQL_INSERT =
            "INSERT INTO public.memory (device_id, name, address, bit) VALUES (?, ?, ?, ?) " +
            "RETURNING id, device_id, name AS area, address, bit";
    private static final String SQL_UPDATE =
            "UPDATE public.memory SET device_id = ?, name = ?, address = ?, bit = ? WHERE id = ? " +
            "RETURNING id, device_id, name AS area, address, bit";
    private static final String SQL_DELETE = "DELETE FROM public.memory WHERE id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Memory> rowMapper = this::mapRow;

    public MemoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Memory> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, Objects.requireNonNull(rowMapper, "rowMapper"));
    }

    public Optional<Memory> findById(int id) {
        return queryOptional(SQL_FIND_BY_ID, id);
    }

    public Memory create(int deviceId, String area, int address, int bit) {
        return jdbcTemplate.queryForObject(SQL_INSERT, Objects.requireNonNull(rowMapper, "rowMapper"), deviceId, area,
                address, bit);
    }

    public Optional<Memory> update(int id, int deviceId, String area, int address, int bit) {
        return queryOptional(SQL_UPDATE, deviceId, area, address, bit, id);
    }

    public boolean delete(int id) {
        return jdbcTemplate.update(SQL_DELETE, id) > 0;
    }

    private Optional<Memory> queryOptional(String sql, Object... args) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(Objects.requireNonNull(sql, "sql"),
                    Objects.requireNonNull(rowMapper, "rowMapper"), args));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Memory mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Memory(
                rs.getInt("id"),
                rs.getInt("device_id"),
                rs.getString("area"),
                rs.getInt("address"),
                rs.getInt("bit"));
    }

    public List<MemoryValueByDeviceDTO> findByDeviceMnemonic(String mnemonic) {
        String sql = "SELECT d.id AS device_id, d.mnemonic AS plc_mnemonic, t.name AS tag_name, " +
                     " " + MEMORY_KEY_SQL + " AS memory_name, mv.value AS value, mv.updated_at AS timestamp " +
                     "FROM public.tag t " +
                     "JOIN public.machine mc ON mc.id = t.machine_id " +
                     "JOIN public.device d ON d.id = mc.device_id " +
                     "JOIN public.memory m ON m.id = t.memory_id " +
                     "JOIN public.memory_value mv ON mv.memory_id = m.id " +
                     "WHERE d.mnemonic = ? " +
                     "ORDER BY mv.updated_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MemoryValueByDeviceDTO dto = new MemoryValueByDeviceDTO();
            dto.setDeviceId(rs.getInt("device_id"));
            dto.setPlcMnemonic(rs.getString("plc_mnemonic"));
            dto.setTagName(rs.getString("tag_name"));
            dto.setMemoryName(rs.getString("memory_name"));
            dto.setValue(rs.getInt("value"));
            Timestamp timestamp = rs.getTimestamp("timestamp");
            dto.setTimestamp(timestamp == null ? null : timestamp.toLocalDateTime());
            return dto;
        }, mnemonic);
    }

    public List<MemoryValueByDeviceDTO> findByStructureFilters(
            Long fabricaId,
            Long miniFabricaId,
            Long setorId,
            Long machineId) {
        String sql = "SELECT d.id AS device_id, d.mnemonic AS plc_mnemonic, t.name AS tag_name, " +
                " " + MEMORY_KEY_SQL + " AS memory_name, mv.value AS value, mv.updated_at AS timestamp " +
                "FROM public.tag t " +
                "JOIN public.machine mc ON mc.id = t.machine_id " +
                "JOIN public.device d ON d.id = mc.device_id " +
                "JOIN public.memory m ON m.id = t.memory_id " +
                "JOIN public.memory_value mv ON mv.memory_id = m.id " +
                "LEFT JOIN public.mini_fabrica mf ON mf.id = mc.mini_fabrica_id " +
                "WHERE (?::bigint IS NULL OR mf.fabrica_id = ?::bigint) " +
                "AND (?::bigint IS NULL OR mc.mini_fabrica_id = ?::bigint) " +
                "AND (?::bigint IS NULL OR mc.setor_id = ?::bigint) " +
                "AND (?::bigint IS NULL OR mc.id = ?::bigint) " +
                "ORDER BY mv.updated_at DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            MemoryValueByDeviceDTO dto = new MemoryValueByDeviceDTO();
            dto.setDeviceId(rs.getInt("device_id"));
            dto.setPlcMnemonic(rs.getString("plc_mnemonic"));
            dto.setTagName(rs.getString("tag_name"));
            dto.setMemoryName(rs.getString("memory_name"));
            dto.setValue(rs.getInt("value"));
            Timestamp timestamp = rs.getTimestamp("timestamp");
            dto.setTimestamp(timestamp == null ? null : timestamp.toLocalDateTime());
            return dto;
        }, fabricaId, fabricaId, miniFabricaId, miniFabricaId, setorId, setorId, machineId, machineId);
    }
}
