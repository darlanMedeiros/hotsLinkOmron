package org.ctrl.db.api;

import java.util.List;
import java.util.Optional;

import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.DmValue;
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
    public ResponseEntity<DmValue> getByAddress(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<DmValue> value = service.getByAddress(device, address);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm/{address:\\d+}/last")
    public ResponseEntity<DmValue> getLatestByAddress(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<DmValue> value = service.getByAddress(device, address);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm/{address:\\d+}/current")
    public ResponseEntity<DmValue> getCurrentByAddress(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<DmValue> value = service.getCurrentByAddress(device, address);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm/last")
    public ResponseEntity<DmValue> getLatest(
            @PathVariable String mnemonic) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<DmValue> value = service.getLatest(device);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/dm")
    public List<DmValue> getRange(
            @PathVariable String mnemonic,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "1000") int end) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        return service.getRange(device, start, end);
    }
}
