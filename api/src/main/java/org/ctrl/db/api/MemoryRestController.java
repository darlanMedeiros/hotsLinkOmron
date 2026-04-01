package org.ctrl.db.api;

import java.util.List;

import org.ctrl.db.api.dto.MemoryValueByDeviceDTO;
import org.ctrl.db.api.repository.MemoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class MemoryRestController {

    private final MemoryRepository memoryRepository;

    public MemoryRestController(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @GetMapping("/{mnemonic}/memory-values")
    public ResponseEntity<List<MemoryValueByDeviceDTO>> getMemoryValuesByDevice(@PathVariable String mnemonic) {
        List<MemoryValueByDeviceDTO> memoryValues = memoryRepository.findByDeviceMnemonic(mnemonic);
        if (memoryValues.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(memoryValues);
    }

    @GetMapping("/memory-values/structured")
    public ResponseEntity<List<MemoryValueByDeviceDTO>> getMemoryValuesByStructure(
            @RequestParam(required = false) Long fabricaId,
            @RequestParam(required = false) Long miniFabricaId,
            @RequestParam(required = false) Long setorId,
            @RequestParam(required = false) Long machineId) {
        List<MemoryValueByDeviceDTO> memoryValues = memoryRepository.findByStructureFilters(
                fabricaId,
                miniFabricaId,
                setorId,
                machineId);
        if (memoryValues.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(memoryValues);
    }
}
