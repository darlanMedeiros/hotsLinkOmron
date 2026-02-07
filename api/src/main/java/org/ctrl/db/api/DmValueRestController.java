package org.ctrl.db.api;

import java.util.List;
import java.util.Optional;

import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.db.service.DmValueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DmValueRestController {

    private final DmValueService service;

    public DmValueRestController(DmValueService service) {
        this.service = service;
    }

    @GetMapping("/devices/{mnemonic}/dm/{address:\\d+}")
    public ResponseEntity<MemoryValue> getByAddress(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<MemoryValue> value = service.getByAddress(device, address);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm/{address:\\d+}/last")
    public ResponseEntity<MemoryValue> getLatestByAddress(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<MemoryValue> value = service.getByAddress(device, address);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm/{address:\\d+}/current")
    public ResponseEntity<MemoryValue> getCurrentByAddress(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<MemoryValue> value = service.getCurrentByAddress(device, address);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm/last")
    public ResponseEntity<MemoryValue> getLatest(
            @PathVariable String mnemonic) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<MemoryValue> value = service.getLatest(device);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm")
    public List<MemoryValue> getRange(
            @PathVariable String mnemonic,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "1000") int end) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        return service.getRange(device, start, end);
    }
}
