package org.ctrl.db.controller;

import org.ctrl.db.model.DeviceInfo;
import org.ctrl.db.model.Tag;
import org.ctrl.db.service.TagService;

public class TagController {

    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
    }

    public Tag createTag(DeviceInfo device, String tagName, String memoryName) {
        return service.createTag(device, tagName, memoryName);
    }

    public Tag createDmTag(DeviceInfo device, String tagName, int address) {
        return service.createDmTag(device, tagName, address);
    }

    public Tag createRrTag(DeviceInfo device, String tagName, int address, int bit) {
        return service.createRrTag(device, tagName, address, bit);
    }
}
