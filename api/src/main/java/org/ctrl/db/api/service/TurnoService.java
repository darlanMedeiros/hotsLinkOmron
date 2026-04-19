package org.ctrl.db.api.service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.Turno;
import org.ctrl.db.repository.TurnoRepository;
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
        LocalTime start = parseTime(horaInicio, "horaInicio");
        LocalTime end = parseTime(horaFinal, "horaFinal");
        Long id = repository.create(requireName(name), start, end);
        return new Turno(id, name, start, end);
    }

    public Optional<Turno> update(long id, String name, String horaInicio, String horaFinal) {
        validateId(id, "id");
        LocalTime start = parseTime(horaInicio, "horaInicio");
        LocalTime end = parseTime(horaFinal, "horaFinal");
        repository.update(id, requireName(name), start, end);
        return Optional.of(new Turno(id, name, start, end));
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

    private LocalTime parseTime(String time, String fieldName) {
        if (time == null || time.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        try {
            return LocalTime.parse(time.trim());
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

