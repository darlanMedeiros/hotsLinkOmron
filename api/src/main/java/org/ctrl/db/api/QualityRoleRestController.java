package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.QualityRoleRequest;
import org.ctrl.db.api.model.QualityRoleOption;
import org.ctrl.db.api.service.QualityRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tags/quality-roles")
public class QualityRoleRestController {

    private final QualityRoleService service;

    public QualityRoleRestController(QualityRoleService service) {
        this.service = service;
    }

    @GetMapping
    public List<QualityRoleOption> findAll(@RequestParam(name = "activeOnly", required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return service.findActive();
        }
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<QualityRoleOption> findById(@PathVariable int id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<QualityRoleOption> create(@RequestBody QualityRoleRequest request) {
        QualityRoleOption created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getActive());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<QualityRoleOption> update(@PathVariable int id, @RequestBody QualityRoleRequest request) {
        return service.update(
                id,
                request == null ? null : request.getName(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getActive())
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

