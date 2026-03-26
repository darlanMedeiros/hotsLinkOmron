package org.ctrl.db.api.service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.api.model.Turno;
import org.ctrl.db.api.repository.TurnoRepository;
import org.springframework.stereotype.Service;

@Service
public class TurnoService {

    private final TurnoRepository repository;

    public TurnoService(TurnoRepository repository) {
        this.repository = repository;
    }

    public List<Turno> findAll() {
        return repository.findAll();
    }

    public Optional<Turno> findById(long id) {
        validateId(id, "id");
        return repository.findById(id);
    }

    public Turno create(String name, String horaInicio, String horaFinal) {
        return repository.create(requireName(name), requireTime(horaInicio, "horaInicio"), requireTime(horaFinal, "horaFinal"));
    }

    public Optional<Turno> update(long id, String name, String horaInicio, String horaFinal) {
        validateId(id, "id");
        return repository.update(id, requireName(name), requireTime(horaInicio, "horaInicio"), requireTime(horaFinal, "horaFinal"));
    }

    public boolean delete(long id) {
        validateId(id, "id");
        return repository.delete(id);
    }

    private String requireName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        return name.trim();
    }

    private String requireTime(String time, String fieldName) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            LocalTime parsed = LocalTime.parse(time.trim());
            return parsed.toString();
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must be a valid time (HH:mm or HH:mm:ss)");
        }
    }

    private void validateId(long id, String fieldName) {
        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
    }
}
