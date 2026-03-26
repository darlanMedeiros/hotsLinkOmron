package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.TurnoRequest;
import org.ctrl.db.api.model.Turno;
import org.ctrl.db.api.service.TurnoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/turnos")
public class TurnoRestController {

    private final TurnoService service;

    public TurnoRestController(TurnoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Turno> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Turno> findById(@PathVariable long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Turno> create(@RequestBody TurnoRequest request) {
        Turno created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getHoraInicio(),
                request == null ? null : request.getHoraFinal());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Turno> update(@PathVariable long id, @RequestBody TurnoRequest request) {
        return service.update(
                        id,
                        request == null ? null : request.getName(),
                        request == null ? null : request.getHoraInicio(),
                        request == null ? null : request.getHoraFinal())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        if (!service.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
