package org.ctrl.db.model;

import java.time.LocalTime;

public class Turno {

    private final Long id;
    private final String name;
    private final LocalTime horaInicio;
    private final LocalTime horaFinal;

    public Turno(Long id, String name, LocalTime horaInicio, LocalTime horaFinal) {
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

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFinal() {
        return horaFinal;
    }
    
    /**
     * Verifica se um determinado horário está dentro deste turno.
     * Cuida de turnos que cruzam a meia-noite (ex: 22h às 06h).
     */
    public boolean contains(LocalTime time) {
        if (horaInicio.isBefore(horaFinal)) {
            // Turno no mesmo dia (ex: 08:00 - 16:00)
            return !time.isBefore(horaInicio) && time.isBefore(horaFinal);
        } else {
            // Turno que cruza meia-noite (ex: 22:00 - 06:00)
            return !time.isBefore(horaInicio) || time.isBefore(horaFinal);
        }
    }
}
