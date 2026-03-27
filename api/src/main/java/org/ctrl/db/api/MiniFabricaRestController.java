package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.MiniFabricaRequest;
import org.ctrl.db.api.model.MiniFabrica;
import org.ctrl.db.api.service.MiniFabricaService;
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
@RequestMapping("/api/mini-fabricas")
public class MiniFabricaRestController {

    private final MiniFabricaService service;

    public MiniFabricaRestController(MiniFabricaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MiniFabrica> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MiniFabrica> findById(@PathVariable long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MiniFabrica> create(@RequestBody MiniFabricaRequest request) {
        MiniFabrica created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getFabricaId(),
                request == null ? null : request.getSetorIds());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<MiniFabrica> update(@PathVariable long id, @RequestBody MiniFabricaRequest request) {
        return service.update(
                id,
                request == null ? null : request.getName(),
                request == null ? null : request.getFabricaId(),
                request == null ? null : request.getSetorIds())
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
