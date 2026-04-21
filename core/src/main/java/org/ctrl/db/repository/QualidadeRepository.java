package org.ctrl.db.repository;

import org.ctrl.db.model.Qualidade;
import org.ctrl.db.model.QualidadeDefeitoValor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QualidadeRepository {

    private final JdbcTemplate jdbcTemplate;

    public QualidadeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void save(Qualidade qualidade) {
        String sqlQualidade = "INSERT INTO public.qualidade (machine_id, value, hora, turno_id) VALUES (?, ?, ?, ?) RETURNING id";

        Long qualidadeId = jdbcTemplate.queryForObject(sqlQualidade, Long.class,
                qualidade.getMachineId(),
                qualidade.getValue(),
                qualidade.getHora(),
                qualidade.getTurnoId());

        qualidade.setId(qualidadeId);

        if (qualidade.getDefeitos() != null && !qualidade.getDefeitos().isEmpty()) {
            String sqlDefeito = "INSERT INTO public.qualidade_defeito_valor (qualidade_id, defeito_id, value) VALUES (?, ?, ?)";
            for (QualidadeDefeitoValor qdv : qualidade.getDefeitos()) {
                jdbcTemplate.update(sqlDefeito, qualidadeId, qdv.getDefeitoId(), qdv.getValue());
            }
        }
    }

    public List<Qualidade> findAll(Long machineId, Long turnoId, LocalDateTime start, LocalDateTime end) {
        StringBuilder sql = new StringBuilder(
                "SELECT q.id, q.machine_id, m.name as machine_name, q.value as qual_value, q.hora, q.turno_id, t.name as turno_name, "
                        +
                        "qdv.id as qdv_id, qdv.defeito_id, d.name as defeito_name, qdv.value as defect_value " +
                        "FROM public.qualidade q " +
                        "JOIN public.machine m ON q.machine_id = m.id " +
                        "JOIN public.turno t ON q.turno_id = t.id " +
                        "LEFT JOIN public.qualidade_defeito_valor qdv ON q.id = qdv.qualidade_id " +
                        "LEFT JOIN public.defeito d ON qdv.defeito_id = d.id " +
                        "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (machineId != null) {
            sql.append("AND q.machine_id = ? ");
            params.add(machineId);
        }
        if (turnoId != null) {
            sql.append("AND q.turno_id = ? ");
            params.add(turnoId);
        }
        if (start != null) {
            sql.append("AND q.hora >= ? ");
            params.add(start);
        }
        if (end != null) {
            sql.append("AND q.hora <= ? ");
            params.add(end);
        }

        sql.append("ORDER BY q.hora DESC, q.id DESC");

        return jdbcTemplate.query(String.valueOf(sql), (ResultSetExtractor<List<Qualidade>>) rs -> {
            Map<Long, Qualidade> map = new LinkedHashMap<>();
            while (rs.next()) {
                Long id = rs.getLong("id");
                Qualidade qual = map.get(id);
                if (qual == null) {
                    qual = new Qualidade(
                            id,
                            rs.getLong("machine_id"),
                            Objects.requireNonNullElse(rs.getString("machine_name"), "N/A"),
                            rs.getInt("qual_value"),
                            rs.getTimestamp("hora").toLocalDateTime(),
                            rs.getLong("turno_id"),
                            Objects.requireNonNullElse(rs.getString("turno_name"), "N/A"));
                    map.put(id, qual);
                }

                Long qdvId = rs.getObject("qdv_id") != null ? rs.getLong("qdv_id") : null;
                if (qdvId != null) {
                    qual.addDefeito(
                            rs.getLong("defeito_id"),
                            Objects.requireNonNullElse(rs.getString("defeito_name"), "Desconhecido"),
                            rs.getInt("defect_value"));
                }
            }
            return new ArrayList<>(map.values());
        }, params.toArray());
    }
}
