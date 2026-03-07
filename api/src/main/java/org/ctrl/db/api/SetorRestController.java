package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.SetorRequest;
import org.ctrl.db.api.model.Setor;
import org.ctrl.db.api.service.SetorService;
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
@RequestMapping("/api/setores")
public class SetorRestController {

    private final SetorService service;

    public SetorRestController(SetorService service) {
        this.service = service;
    }

    @GetMapping
    public List<Setor> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Setor> findById(@PathVariable long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Setor> create(@RequestBody SetorRequest request) {
        Setor created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getMiniFabricaId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Setor> update(@PathVariable long id, @RequestBody SetorRequest request) {
        return service.update(
                id,
                request == null ? null : request.getName(),
                request == null ? null : request.getMiniFabricaId())
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
