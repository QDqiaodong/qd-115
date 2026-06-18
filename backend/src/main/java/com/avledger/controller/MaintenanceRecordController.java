package com.avledger.controller;

import com.avledger.entity.MaintenanceRecord;
import com.avledger.enums.MaintenanceType;
import com.avledger.service.MaintenanceRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
@RequiredArgsConstructor
public class MaintenanceRecordController {

    private final MaintenanceRecordService maintenanceRecordService;
    private final ObjectMapper objectMapper;

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<MaintenanceRecord>> findByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(maintenanceRecordService.findByDeviceId(deviceId));
    }

    @GetMapping
    public ResponseEntity<List<MaintenanceRecord>> findAll(@RequestParam(required = false) Long deviceId) {
        return ResponseEntity.ok(maintenanceRecordService.findAll(deviceId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MaintenanceRecord>> findByType(@PathVariable MaintenanceType type) {
        return ResponseEntity.ok(maintenanceRecordService.findByType(type));
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecord> create(@RequestBody Map<String, Object> payload) {
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        payload.remove("deviceId");
        MaintenanceRecord maintenanceRecord = objectMapper.convertValue(payload, MaintenanceRecord.class);
        return ResponseEntity.ok(maintenanceRecordService.save(maintenanceRecord, deviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceRecord> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Long deviceId = payload.get("deviceId") != null ? Long.valueOf(payload.get("deviceId").toString()) : null;
        payload.remove("deviceId");
        MaintenanceRecord maintenanceRecord = objectMapper.convertValue(payload, MaintenanceRecord.class);
        return maintenanceRecordService.update(id, maintenanceRecord, deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return maintenanceRecordService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
