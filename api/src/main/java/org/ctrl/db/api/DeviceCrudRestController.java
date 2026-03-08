package org.ctrl.db.api;

import java.util.List;
import org.ctrl.db.api.dto.DeviceRequest;
import org.ctrl.db.api.model.Device;
import org.ctrl.db.api.service.DeviceService;
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
@RequestMapping("/api/devices")
public class DeviceCrudRestController {

    private final DeviceService service;

    public DeviceCrudRestController(DeviceService service) {
        this.service = service;
    }

    @GetMapping
    public List<Device> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Device> findById(@PathVariable int id) {
        return service.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Device> create(@RequestBody DeviceRequest request) {
        Device created = service.create(
                request == null ? null : request.getMnemonic(),
                request == null ? null : request.getName(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getNodeId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Device> update(@PathVariable int id, @RequestBody DeviceRequest request) {
        return service.update(
                id,
                request == null ? null : request.getMnemonic(),
                request == null ? null : request.getName(),
                request == null ? null : request.getDescription(),
                request == null ? null : request.getNodeId())
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
