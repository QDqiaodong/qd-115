package com.avledger.controller;

import com.avledger.entity.FirmwareRecord;
import com.avledger.service.FirmwareRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/firmware")
@RequiredArgsConstructor
public class FirmwareRecordController {

    private final FirmwareRecordService firmwareRecordService;
    private final ObjectMapper objectMapper;

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<FirmwareRecord>> findByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(firmwareRecordService.findByDeviceId(deviceId));
    }

    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<FirmwareRecord> findLatestByDeviceId(@PathVariable Long deviceId) {
        return firmwareRecordService.findLatestByDeviceId(deviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FirmwareRecord> findById(@PathVariable Long id) {
        return firmwareRecordService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<FirmwareRecord> create(@RequestBody Map<String, Object> payload) {
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        payload.remove("deviceId");
        FirmwareRecord firmwareRecord = objectMapper.convertValue(payload, FirmwareRecord.class);
        return ResponseEntity.ok(firmwareRecordService.save(firmwareRecord, deviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FirmwareRecord> update(@PathVariable Long id, @RequestBody FirmwareRecord firmwareRecord) {
        return firmwareRecordService.update(id, firmwareRecord)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return firmwareRecordService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
