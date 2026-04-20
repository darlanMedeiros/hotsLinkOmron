package org.ctrl.db.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Qualidade {

    private Long id;
    private Long machineId;
    private String machineName;
    private Integer value;
    private LocalDateTime hora;
    private Long turnoId;
    private String turnoName;
    private List<QualidadeDefeitoValor> defeitos = new ArrayList<>();

    public Qualidade() {}

    public Qualidade(Long id, Long machineId, String machineName, Integer value, LocalDateTime hora, Long turnoId, String turnoName) {
        this.id = id;
        this.machineId = machineId;
        this.machineName = machineName;
        this.value = value;
        this.hora = hora;
        this.turnoId = turnoId;
        this.turnoName = turnoName;
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

    public String getMachineName() {
        return machineName;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public String getTurnoName() {
        return turnoName;
    }

    public void setTurnoName(String turnoName) {
        this.turnoName = turnoName;
    }

    public List<QualidadeDefeitoValor> getDefeitos() {
        return defeitos;
    }

    public void setDefeitos(List<QualidadeDefeitoValor> defeitos) {
        this.defeitos = defeitos;
    }
    
    public void addDefeito(Long defeitoId, String defeitoName, Integer value) {
        this.defeitos.add(new QualidadeDefeitoValor(null, null, defeitoId, defeitoName, value));
    }
}
