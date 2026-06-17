package com.avledger.controller;

import com.avledger.entity.RepairRecord;
import com.avledger.enums.DeviceType;
import com.avledger.service.RepairRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repair")
@RequiredArgsConstructor
public class RepairRecordController {

    private final RepairRecordService repairRecordService;
    private final ObjectMapper objectMapper;

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<RepairRecord>> findByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(repairRecordService.findByDeviceId(deviceId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<RepairRecord>> getRecent() {
        return ResponseEntity.ok(repairRecordService.getRecentRepairs());
    }

    @GetMapping("/timeline/{deviceId}")
    public ResponseEntity<List<Map<String, Object>>> getTimeline(@PathVariable Long deviceId) {
        return ResponseEntity.ok(repairRecordService.getRepairTimeline(deviceId));
    }

    @GetMapping("/cost-statistics")
    public ResponseEntity<Map<String, Object>> getCostStatistics(
            @RequestParam(required = false) DeviceType deviceType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) String cause
    ) {
        return ResponseEntity.ok(repairRecordService.getCostStatistics(deviceType, location, yearMonth, cause));
    }

    @PostMapping
    public ResponseEntity<RepairRecord> create(@RequestBody Map<String, Object> payload) {
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        payload.remove("deviceId");
        RepairRecord repairRecord = objectMapper.convertValue(payload, RepairRecord.class);
        return ResponseEntity.ok(repairRecordService.save(repairRecord, deviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RepairRecord> update(@PathVariable Long id, @RequestBody RepairRecord repairRecord) {
        return repairRecordService.update(id, repairRecord)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return repairRecordService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
