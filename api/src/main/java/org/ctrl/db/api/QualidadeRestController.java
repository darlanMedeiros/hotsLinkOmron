package org.ctrl.db.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.ctrl.db.api.dto.QualidadeHistoryDTO;
import org.ctrl.db.model.Qualidade;
import org.ctrl.db.service.QualidadeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qualidade")
public class QualidadeRestController {

    private final QualidadeService qualidadeService;

    public QualidadeRestController(QualidadeService qualidadeService) {
        this.qualidadeService = qualidadeService;
    }

    @GetMapping("/historico")
    public List<QualidadeHistoryDTO> getHistory(
            @RequestParam(required = false) Long machineId,
            @RequestParam(required = false) Long turnoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        LocalDateTime start = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime end = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;

        List<Qualidade> history = qualidadeService.findHistory(machineId, turnoId, start, end);

        return history.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private QualidadeHistoryDTO convertToDTO(Qualidade q) {
        List<QualidadeHistoryDTO.QualidadeDefeitoDTO> defeitos = q.getDefeitos().stream()
                .map(d -> new QualidadeHistoryDTO.QualidadeDefeitoDTO(d.getDefeitoId(), d.getDefeitoName(),
                        d.getValue(), d.getAmostragem()))
                .collect(Collectors.toList());

        return new QualidadeHistoryDTO(
                q.getId(),
                q.getMachineId(),
                q.getMachineName(),
                q.getValue(),
                q.getHora(),
                q.getTurnoId(),
                q.getTurnoName(),
                defeitos);
    }
}
