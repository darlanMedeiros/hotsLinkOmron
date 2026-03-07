package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.MemoryValueRequest;
import org.ctrl.db.api.model.MemoryValueCrud;
import org.ctrl.db.api.service.MemoryValueCrudService;
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
@RequestMapping("/api/memory-values")
public class MemoryValueCrudRestController {

    private final MemoryValueCrudService service;

    public MemoryValueCrudRestController(MemoryValueCrudService service) {
        this.service = service;
    }

    @GetMapping
    public List<MemoryValueCrud> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<MemoryValueCrud> findById(@PathVariable int id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<MemoryValueCrud> create(@RequestBody MemoryValueRequest request) {
        MemoryValueCrud created = service.create(
                request == null ? null : request.getMemoryId(),
                request == null ? null : request.getValue(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getUpdatedAt());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<MemoryValueCrud> update(@PathVariable int id, @RequestBody MemoryValueRequest request) {
        return service.update(
                id,
                request == null ? null : request.getMemoryId(),
                request == null ? null : request.getValue(),
                request == null ? null : request.getStatus(),
                request == null ? null : request.getUpdatedAt())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        if (!service.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
