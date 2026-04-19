package org.ctrl.db.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Qualidade {

    private Long id;
    private Long machineId;
    private Integer value;
    private LocalDateTime hora;
    private Long turnoId;
    private List<QualidadeDefeitoValor> defeitos = new ArrayList<>();

    public Qualidade() {}

    public Qualidade(Long id, Long machineId, Integer value, LocalDateTime hora, Long turnoId) {
        this.id = id;
        this.machineId = machineId;
        this.value = value;
        this.hora = hora;
        this.turnoId = turnoId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMachineId() {
        return machineId;
    }

    public void setMachineId(Long machineId) {
        this.machineId = machineId;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public LocalDateTime getHora() {
        return hora;
    }

    public void setHora(LocalDateTime hora) {
        this.hora = hora;
    }

    public Long getTurnoId() {
        return turnoId;
    }

    public void setTurnoId(Long turnoId) {
        this.turnoId = turnoId;
    }

    public List<QualidadeDefeitoValor> getDefeitos() {
        return defeitos;
    }

    public void setDefeitos(List<QualidadeDefeitoValor> defeitos) {
        this.defeitos = defeitos;
    }
    
    public void addDefeito(Long defeitoId, Integer value) {
        this.defeitos.add(new QualidadeDefeitoValor(null, null, defeitoId, value));
    }
}
