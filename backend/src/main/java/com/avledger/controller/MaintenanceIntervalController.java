package com.avledger.controller;

import com.avledger.enums.DeviceType;
import com.avledger.enums.MaintenanceType;
import com.avledger.service.MaintenanceIntervalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenance-interval")
@RequiredArgsConstructor
public class MaintenanceIntervalController {

    private final MaintenanceIntervalService maintenanceIntervalService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/intervals")
    public ResponseEntity<Map<String, Integer>> getAllIntervals() {
        return ResponseEntity.ok(maintenanceIntervalService.getAllIntervals());
    }

    @GetMapping("/intervals/{deviceType}")
    public ResponseEntity<Map<String, Integer>> getIntervalsByDeviceType(@PathVariable DeviceType deviceType) {
        return ResponseEntity.ok(maintenanceIntervalService.getIntervalsByDeviceType(deviceType));
    }

    @GetMapping("/next-window")
    public ResponseEntity<Map<String, Object>> getNextWindow(
            @RequestParam DeviceType deviceType,
            @RequestParam String lastMaintenanceTime,
            @RequestParam MaintenanceType maintenanceType) {
        LocalDateTime lastTime = LocalDateTime.parse(lastMaintenanceTime, FORMATTER);
        LocalDateTime nextTime = maintenanceIntervalService.calculateNextWindow(deviceType, lastTime, maintenanceType);
        int intervalDays = maintenanceIntervalService.getIntervalDays(deviceType, maintenanceType);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceType", deviceType.name());
        result.put("maintenanceType", maintenanceType.name());
        result.put("intervalDays", intervalDays);
        result.put("lastMaintenanceTime", lastTime.toString());
        result.put("nextTime", nextTime != null ? nextTime.toString() : null);
        if (nextTime != null) {
            result.put("overdue", LocalDateTime.now().isAfter(nextTime));
            result.put("daysUntil", java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), nextTime));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/next-windows")
    public ResponseEntity<Map<String, Object>> getNextWindows(
            @RequestParam DeviceType deviceType,
            @RequestParam(required = false) String lastMaintenanceTime) {
        LocalDateTime lastTime = null;
        if (lastMaintenanceTime != null && !lastMaintenanceTime.isEmpty()) {
            lastTime = LocalDateTime.parse(lastMaintenanceTime, FORMATTER);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceType", deviceType.name());
        result.put("lastMaintenanceTime", lastTime != null ? lastTime.toString() : null);
        result.put("windows", maintenanceIntervalService.calculateNextWindows(deviceType, lastTime));
        return ResponseEntity.ok(result);
    }
}
