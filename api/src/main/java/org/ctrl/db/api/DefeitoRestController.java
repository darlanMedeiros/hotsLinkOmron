package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.DefeitoRequest;
import org.ctrl.db.api.model.Defeito;
import org.ctrl.db.api.service.DefeitoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        Defeito created = service.create(request.getName(), request.getNumber());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Defeito> update(@PathVariable long id, @RequestBody DefeitoRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }
        return service.update(id, request.getName(), request.getNumber())
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
