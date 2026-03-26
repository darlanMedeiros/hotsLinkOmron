package org.ctrl.db.api.model;

public class Turno {

    private final Long id;
    private final String name;
    private final String horaInicio;
    private final String horaFinal;

    public Turno(Long id, String name, String horaInicio, String horaFinal) {
        this.id = id;
        this.name = name;
        this.horaInicio = horaInicio;
        this.horaFinal = horaFinal;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public String getHoraFinal() {
        return horaFinal;
    }
}
