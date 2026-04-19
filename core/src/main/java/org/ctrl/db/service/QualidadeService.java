package org.ctrl.db.service;

import java.time.LocalDateTime;
import java.util.Optional;
import org.ctrl.db.model.Qualidade;
import org.ctrl.db.model.Turno;
import org.ctrl.db.repository.QualidadeRepository;
import org.ctrl.db.repository.TurnoRepository;
import org.springframework.stereotype.Service;

@Service
public class QualidadeService {

    private final QualidadeRepository qualidadeRepository;
    private final TurnoRepository turnoRepository;

    public QualidadeService(QualidadeRepository qualidadeRepository, TurnoRepository turnoRepository) {
        this.qualidadeRepository = qualidadeRepository;
        this.turnoRepository = turnoRepository;
    }

    public void saveWithShiftDetection(Qualidade qualidade) {
        if (qualidade.getHora() == null) {
            qualidade.setHora(LocalDateTime.now());
        }

        // Detecta o turno com base no horário (timestamp do CLP)
        Optional<Turno> turno = turnoRepository.findTurnoByTime(qualidade.getHora().toLocalTime());
        
        if (turno.isPresent()) {
            qualidade.setTurnoId(turno.get().getId());
            qualidadeRepository.save(qualidade);
            System.out.println("INFO: Registro de qualidade salvo com sucesso. Turno: " + turno.get().getName());
        } else {
            // Se não encontrar turno, NÃO podemos salvar pois o banco exige turno_id (NOT NULL)
            System.err.println("ERRO CRÍTICO: Turno não encontrado para o horário " + qualidade.getHora() + ". Registro de qualidade ignorado.");
        }
    }
}
