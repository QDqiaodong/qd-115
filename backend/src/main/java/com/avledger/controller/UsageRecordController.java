package com.avledger.controller;

import com.avledger.entity.UsageRecord;
import com.avledger.service.UsageRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usage")
@RequiredArgsConstructor
public class UsageRecordController {

    private final UsageRecordService usageRecordService;
    private final ObjectMapper objectMapper;

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<UsageRecord>> findByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(usageRecordService.findByDeviceId(deviceId));
    }

    @GetMapping("/device/{deviceId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long deviceId) {
        return ResponseEntity.ok(usageRecordService.getUsageStatsByDevice(deviceId));
    }

    @PostMapping
    public ResponseEntity<UsageRecord> create(@RequestBody Map<String, Object> payload) {
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        payload.remove("deviceId");
        UsageRecord usageRecord = objectMapper.convertValue(payload, UsageRecord.class);
        return ResponseEntity.ok(usageRecordService.save(usageRecord, deviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsageRecord> update(@PathVariable Long id, @RequestBody UsageRecord usageRecord) {
        return usageRecordService.update(id, usageRecord)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return usageRecordService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
