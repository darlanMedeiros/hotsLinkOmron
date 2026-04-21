package org.ctrl.db.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public class QualidadeHistoryDTO {
    private Long id;
    private Long machineId;
    private String machineName;
    private Integer value;
    private LocalDateTime hora;
    private Long turnoId;
    private String turnoName;
    private Double qualidadeParcial;
    private List<QualidadeDefeitoDTO> defeitos;

    public QualidadeHistoryDTO() {}

    public QualidadeHistoryDTO(Long id, Long machineId, String machineName, Integer value, LocalDateTime hora, Long turnoId, String turnoName, Double qualidadeParcial, List<QualidadeDefeitoDTO> defeitos) {
        this.id = id;
        this.machineId = machineId;
        this.machineName = machineName;
        this.value = value;
        this.hora = hora;
        this.turnoId = turnoId;
        this.turnoName = turnoName;
        this.qualidadeParcial = qualidadeParcial;
        this.defeitos = defeitos;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMachineId() { return machineId; }
    public void setMachineId(Long machineId) { this.machineId = machineId; }
    public String getMachineName() { return machineName; }
    public void setMachineName(String machineName) { this.machineName = machineName; }
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
    public LocalDateTime getHora() { return hora; }
    public void setHora(LocalDateTime hora) { this.hora = hora; }
    public Long getTurnoId() { return turnoId; }
    public void setTurnoId(Long turnoId) { this.turnoId = turnoId; }
    public String getTurnoName() { return turnoName; }
    public void setTurnoName(String turnoName) { this.turnoName = turnoName; }
    public Double getQualidadeParcial() { return qualidadeParcial; }
    public void setQualidadeParcial(Double qualidadeParcial) { this.qualidadeParcial = qualidadeParcial; }
    public List<QualidadeDefeitoDTO> getDefeitos() { return defeitos; }
    public void setDefeitos(List<QualidadeDefeitoDTO> defeitos) { this.defeitos = defeitos; }

    public static class QualidadeDefeitoDTO {
        private Long defeitoId;
        private String defeitoName;
        private Integer value;
        private Integer amostragem;

        public QualidadeDefeitoDTO() {}

        public QualidadeDefeitoDTO(Long defeitoId, String defeitoName, Integer value, Integer amostragem) {
            this.defeitoId = defeitoId;
            this.defeitoName = defeitoName;
            this.value = value;
            this.amostragem = amostragem;
        }

        public Long getDefeitoId() { return defeitoId; }
        public void setDefeitoId(Long defeitoId) { this.defeitoId = defeitoId; }
        public String getDefeitoName() { return defeitoName; }
        public void setDefeitoName(String defeitoName) { this.defeitoName = defeitoName; }
        public Integer getValue() { return value; }
        public void setValue(Integer value) { this.value = value; }
        public Integer getAmostragem() { return amostragem; }
        public void setAmostragem(Integer amostragem) { this.amostragem = amostragem; }
    }
}
