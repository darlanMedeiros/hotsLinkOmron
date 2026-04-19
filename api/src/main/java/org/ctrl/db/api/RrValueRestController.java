package org.ctrl.db.api;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.MemoryValue;
import org.ctrl.db.service.RrValueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RrValueRestController {

    private final RrValueService service;

    public RrValueRestController(RrValueService service) {
        this.service = service;
    }

    @GetMapping("/devices/{mnemonic}/rr/{address:\\d+}/bit/{bit:\\d+}")
    public ResponseEntity<MemoryValue> getCurrentBit(
            @PathVariable String mnemonic,
            @PathVariable int address,
            @PathVariable int bit) {
        DeviceInfo device = new DeviceInfo(0, mnemonic, "", "");
        Optional<MemoryValue> value = service.getCurrent(device, address, bit);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/rr/10/bit/0")
    public ResponseEntity<MemoryValue> getBit1000(
            @PathVariable String mnemonic) {
        DeviceInfo device = new DeviceInfo(0, mnemonic, "", "");
        Optional<MemoryValue> value = service.getCurrent(device, 10, 0);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/rr/{address:\\d+}/bits")
    public List<MemoryValue> getBitsRange(
            @PathVariable String mnemonic,
            @PathVariable int address,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "15") int end) {
        DeviceInfo device = new DeviceInfo(0, mnemonic, "", "");
        return service.getRangeCurrent(device, address, start, end);
    }

    @GetMapping("/devices/{mnemonic}/rr/{address:\\d+}")
    public List<MemoryValue> getAllBits(
            @PathVariable String mnemonic,
            @PathVariable int address) {
        DeviceInfo device = new DeviceInfo(0, mnemonic, "", "");
        return service.getRangeCurrent(device, address, 0, 15);
    }
}
