package org.ctrl.db.api;

import java.util.List;
import java.util.Optional;
import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.RrValue;
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
    public ResponseEntity<RrValue> getCurrentBit(
            @PathVariable String mnemonic,
            @PathVariable int address,
            @PathVariable int bit) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<RrValue> value = service.getCurrent(device, address, bit);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/rr/10/bit/0")
    public ResponseEntity<RrValue> getBit1000(
            @PathVariable String mnemonic) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Optional<RrValue> value = service.getCurrent(device, 10, 0);
        return value.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/devices/{mnemonic}/rr/{address:\\d+}/bits")
    public List<RrValue> getBitsRange(
            @PathVariable String mnemonic,
            @PathVariable int address,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "15") int end) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        return service.getRangeCurrent(device, address, start, end);
    }
}
