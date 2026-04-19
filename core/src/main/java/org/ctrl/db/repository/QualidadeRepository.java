package org.ctrl.db.repository;

import org.ctrl.db.model.Qualidade;
import org.ctrl.db.model.QualidadeDefeitoValor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Repository
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
}
