package org.ctrl.db.api;

import org.ctrl.db.api.dto.CollectorStatusResponse;
import org.ctrl.db.api.service.CollectorProcessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collector")
public class CollectorControlRestController {

    private final CollectorProcessService collectorProcessService;

    public CollectorControlRestController(CollectorProcessService collectorProcessService) {
        this.collectorProcessService = collectorProcessService;
    }

    @GetMapping("/status")
    public ResponseEntity<CollectorStatusResponse> status() {
        return ResponseEntity.ok(collectorProcessService.status());
    }

    @PostMapping("/start")
    public ResponseEntity<CollectorStatusResponse> start() {
        return ResponseEntity.ok(collectorProcessService.start());
    }

    @PostMapping("/stop")
    public ResponseEntity<CollectorStatusResponse> stop() {
        return ResponseEntity.ok(collectorProcessService.stop());
    }
}
