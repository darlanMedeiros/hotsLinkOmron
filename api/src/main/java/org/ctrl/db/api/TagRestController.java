package org.ctrl.db.api;

import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.Tag;
import org.ctrl.db.model.TagValue;
import org.ctrl.db.service.TagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TagRestController {

    private final TagService service;

    public TagRestController(TagService service) {
        this.service = service;
    }

    @PostMapping("/devices/{mnemonic}/tags/dm/{address:\\d+}")
    public ResponseEntity<Tag> createDmTag(
            @PathVariable String mnemonic,
            @PathVariable int address,
            @RequestParam String name) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Tag tag = service.createDmTag(device, name, address);
        return ResponseEntity.ok(tag);
    }

    @PostMapping("/devices/{mnemonic}/tags/rr/{address:\\d+}/bit/{bit:\\d+}")
    public ResponseEntity<Tag> createRrTag(
            @PathVariable String mnemonic,
            @PathVariable int address,
            @PathVariable int bit,
            @RequestParam String name) {
        DeviceInfo device = new DeviceInfo(mnemonic, "", "");
        Tag tag = service.createRrTag(device, name, address, bit);
        return ResponseEntity.ok(tag);
    }

    @GetMapping("/devices/{mnemonic}/tag/{name}")
    public ResponseEntity<TagValue> getTagValue(
            @PathVariable String mnemonic,
            @PathVariable String name) {
        return service.findCurrentByTag(mnemonic, name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
