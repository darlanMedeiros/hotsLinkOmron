package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.TagCrudRequest;
import org.ctrl.db.api.model.TagCrud;
import org.ctrl.db.api.service.TagCrudService;
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
@RequestMapping("/api/tags")
public class TagCrudRestController {

    private final TagCrudService service;

    public TagCrudRestController(TagCrudService service) {
        this.service = service;
    }

    @GetMapping
    public List<TagCrud> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<TagCrud> findById(@PathVariable int id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TagCrud> create(@RequestBody TagCrudRequest request) {
        TagCrud created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getMachineId(),
                request == null ? null : request.getMemoryId(),
                request == null ? null : request.getPersistHistory());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<TagCrud> update(@PathVariable int id, @RequestBody TagCrudRequest request) {
        return service.update(
                id,
                request == null ? null : request.getName(),
                request == null ? null : request.getMachineId(),
                request == null ? null : request.getMemoryId(),
                request == null ? null : request.getPersistHistory())
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
