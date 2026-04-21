package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.DefeitoRequest;
import org.ctrl.db.api.service.DefeitoService;
import org.ctrl.db.model.Defeito;
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
@RequestMapping("/api/defeitos")
public class DefeitoRestController {

    private final DefeitoService service;

    public DefeitoRestController(DefeitoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Defeito> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Defeito> findById(@PathVariable long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Defeito> create(@RequestBody DefeitoRequest request) {
        Defeito created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getNumber());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Defeito> update(@PathVariable long id, @RequestBody DefeitoRequest request) {
        return service.update(
                id,
                request == null ? null : request.getName(),
                request == null ? null : request.getNumber())
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
