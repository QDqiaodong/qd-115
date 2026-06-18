package com.avledger.controller;

import com.avledger.entity.UsageRecord;
import com.avledger.enums.DeviceType;
import com.avledger.service.UsageRecordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping
    public ResponseEntity<List<UsageRecord>> findAll(@RequestParam(required = false) Long deviceId) {
        return ResponseEntity.ok(usageRecordService.findAll(deviceId));
    }

    @GetMapping("/device/{deviceId}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable Long deviceId) {
        return ResponseEntity.ok(usageRecordService.getUsageStatsByDevice(deviceId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatsOptional(@RequestParam(required = false) Long deviceId) {
        return ResponseEntity.ok(usageRecordService.getUsageStats(deviceId));
    }

    @GetMapping("/heatmap")
    public ResponseEntity<List<Map<String, Object>>> getHeatmap(
            @RequestParam(required = false) Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(usageRecordService.getHeatmapData(deviceId, startDate, endDate));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<Map<String, Object>>> getDailyDetails(
            @RequestParam(required = false) Long deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(usageRecordService.getDailyDetails(deviceId, date));
    }

    @GetMapping("/scene-distribution")
    public ResponseEntity<Map<String, Object>> getSceneDistribution(
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String location) {
        DeviceType type = null;
        if (deviceType != null && !deviceType.isBlank()) {
            type = DeviceType.valueOf(deviceType);
        }
        String loc = (location != null && !location.isBlank()) ? location : null;
        List<Map<String, Object>> result = usageRecordService.getSceneDistribution(type, loc);
        return ResponseEntity.ok(result.isEmpty() ? Map.of("totalMinutes", 0, "items", List.of()) : result.get(0));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<String>> getLocations() {
        return ResponseEntity.ok(usageRecordService.getDistinctLocations());
    }

    @PostMapping
    public ResponseEntity<UsageRecord> create(@RequestBody Map<String, Object> payload) {
        Long deviceId = Long.valueOf(payload.get("deviceId").toString());
        payload.remove("deviceId");
        UsageRecord usageRecord = objectMapper.convertValue(payload, UsageRecord.class);
        return ResponseEntity.ok(usageRecordService.save(usageRecord, deviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsageRecord> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Long deviceId = payload.get("deviceId") != null ? Long.valueOf(payload.get("deviceId").toString()) : null;
        payload.remove("deviceId");
        UsageRecord usageRecord = objectMapper.convertValue(payload, UsageRecord.class);
        return usageRecordService.update(id, usageRecord, deviceId)
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
