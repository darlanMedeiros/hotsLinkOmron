package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.MachineRequest;
import org.ctrl.db.api.model.Machine;
import org.ctrl.db.api.service.MachineService;
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
@RequestMapping("/api/machines")
public class MachineRestController {

    private final MachineService service;

    public MachineRestController(MachineService service) {
        this.service = service;
    }

    @GetMapping
    public List<Machine> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Machine> findById(@PathVariable long id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Machine> create(@RequestBody MachineRequest request) {
        Machine created = service.create(
                request == null ? null : request.getName(),
                request == null ? null : request.getDeviceId(),
                request == null ? null : request.getSetorId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Machine> update(@PathVariable long id, @RequestBody MachineRequest request) {
        return service.update(
                id,
                request == null ? null : request.getName(),
                request == null ? null : request.getDeviceId(),
                request == null ? null : request.getSetorId())
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
