package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.MemoryRequest;
import org.ctrl.db.api.model.Memory;
import org.ctrl.db.api.service.MemoryService;
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
@RequestMapping("/api/memories")
public class MemoryCrudRestController {

    private final MemoryService service;

    public MemoryCrudRestController(MemoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<Memory> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Memory> findById(@PathVariable int id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Memory> create(@RequestBody MemoryRequest request) {
        Memory created = service.create(
                request == null ? null : request.getDeviceId(),
                request == null ? null : request.getName(),
                request == null ? null : request.getAddress());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Memory> update(@PathVariable int id, @RequestBody MemoryRequest request) {
        return service.update(
                id,
                request == null ? null : request.getDeviceId(),
                request == null ? null : request.getName(),
                request == null ? null : request.getAddress())
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
