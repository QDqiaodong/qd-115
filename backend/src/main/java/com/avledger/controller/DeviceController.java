package com.avledger.controller;

import com.avledger.entity.Device;
import com.avledger.entity.FirmwareRecord;
import com.avledger.entity.MaintenanceRecord;
import com.avledger.entity.RepairRecord;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import com.avledger.repository.FirmwareRecordRepository;
import com.avledger.repository.MaintenanceRecordRepository;
import com.avledger.repository.RepairRecordRepository;
import com.avledger.service.DeviceService;
import com.avledger.service.MaintenanceIntervalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final RepairRecordRepository repairRecordRepository;
    private final FirmwareRecordRepository firmwareRecordRepository;
    private final MaintenanceIntervalService maintenanceIntervalService;

    @GetMapping
    public ResponseEntity<List<Device>> findAll() {
        return ResponseEntity.ok(deviceService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Device> findById(@PathVariable Long id) {
        return deviceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Device>> findByType(@PathVariable DeviceType type) {
        return ResponseEntity.ok(deviceService.findByDeviceType(type));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Device>> findByStatus(@PathVariable DeviceStatus status) {
        return ResponseEntity.ok(deviceService.findByStatus(status));
    }

    @GetMapping("/{id}/allowed-transitions")
    public ResponseEntity<List<DeviceStatus>> getAllowedTransitions(@PathVariable Long id) {
        return deviceService.findById(id)
                .map(device -> ResponseEntity.ok(deviceService.getAllowedTransitions(device.getStatus())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/transition")
    public ResponseEntity<Device> transition(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String targetStatus = body.get("targetStatus");
        if (targetStatus == null) {
            return ResponseEntity.badRequest().build();
        }
        DeviceStatus target;
        try {
            target = DeviceStatus.valueOf(targetStatus);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return deviceService.findById(id)
                .map(device -> {
                    if (!deviceService.isTransitionAllowed(device.getStatus(), target)) {
                        throw new IllegalStateException(
                                "设备状态不允许从 " + device.getStatus().name() + " 转换为 " + target.name() +
                                "，退役设备不可恢复为使用或保养状态");
                    }
                    device.setStatus(target);
                    return ResponseEntity.ok(deviceService.save(device));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Device> create(@RequestBody Device device) {
        return ResponseEntity.ok(deviceService.save(device));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Device> update(@PathVariable Long id, @RequestBody Device device) {
        return deviceService.update(id, device)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return deviceService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PutMapping("/batch")
    public ResponseEntity<List<Device>> batchUpdate(@RequestBody List<Device> devices) {
        return ResponseEntity.ok(deviceService.batchUpdate(devices));
    }

    @GetMapping("/maintenance-summaries")
    public ResponseEntity<Map<Long, Map<String, Object>>> getMaintenanceSummaries() {
        List<Device> allDevices = deviceService.findAll();
        Map<Long, Map<String, Object>> summaries = new LinkedHashMap<>();
        for (Device d : allDevices) {
            Map<String, Object> info = new LinkedHashMap<>();
            Optional<MaintenanceRecord> lastMaintenance = maintenanceRecordRepository
                    .findTopByDeviceIdOrderByMaintenanceTimeDesc(d.getId());
            lastMaintenance.ifPresent(m -> {
                info.put("lastMaintenanceTime", m.getMaintenanceTime().toString());
                info.put("lastMaintenanceType", m.getMaintenanceType().name());
            });
            info.put("nextMaintenanceWindows", maintenanceIntervalService.calculateNextWindows(
                    d.getDeviceType(),
                    lastMaintenance.map(MaintenanceRecord::getMaintenanceTime).orElse(null)));
            summaries.put(d.getId(), info);
        }
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/status-wall")
    public ResponseEntity<List<Map<String, Object>>> getStatusWall() {
        List<Device> allDevices = deviceService.findAll();
        Map<DeviceType, List<Device>> grouped = allDevices.stream()
                .collect(Collectors.groupingBy(Device::getDeviceType));

        List<Map<String, Object>> result = new ArrayList<>();
        for (DeviceType type : DeviceType.values()) {
            List<Device> devicesOfType = grouped.getOrDefault(type, List.of());
            List<Map<String, Object>> deviceItems = new ArrayList<>();
            for (Device d : devicesOfType) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", d.getId());
                item.put("name", d.getName());
                item.put("model", d.getModel());
                item.put("status", d.getStatus().name());
                item.put("location", d.getLocation());

                Optional<MaintenanceRecord> lastMaintenance = maintenanceRecordRepository
                        .findTopByDeviceIdOrderByMaintenanceTimeDesc(d.getId());
                lastMaintenance.ifPresent(m -> {
                    item.put("lastMaintenanceTime", m.getMaintenanceTime().toString());
                    item.put("lastMaintenanceContent", m.getContent());
                    item.put("lastMaintenanceType", m.getMaintenanceType().name());
                });

                item.put("nextMaintenanceWindows", maintenanceIntervalService.calculateNextWindows(
                        d.getDeviceType(),
                        lastMaintenance.map(MaintenanceRecord::getMaintenanceTime).orElse(null)));

                Optional<RepairRecord> lastRepair = repairRecordRepository
                        .findTopByDeviceIdOrderByRepairTimeDesc(d.getId());
                lastRepair.ifPresent(r -> {
                    item.put("lastRepairTime", r.getRepairTime().toString());
                    item.put("lastRepairSymptom", r.getSymptom());
                });

                Optional<FirmwareRecord> lastFirmware = firmwareRecordRepository
                        .findTopByDeviceIdOrderByUpdateTimeDesc(d.getId());
                lastFirmware.ifPresent(f -> {
                    item.put("lastFirmwareVersion", f.getFirmwareVersion());
                    item.put("lastFirmwareUpdateTime", f.getUpdateTime().toString());
                    item.put("lastFirmwareDescription", f.getDescription());
                });

                deviceItems.add(item);
            }

            Map<String, Object> group = new LinkedHashMap<>();
            group.put("deviceType", type.name());
            group.put("devices", deviceItems);

            Map<String, Long> statusCounts = devicesOfType.stream()
                    .collect(Collectors.groupingBy(d -> d.getStatus().name(), Collectors.counting()));
            group.put("statusCounts", statusCounts);
            group.put("totalCount", devicesOfType.size());

            result.add(group);
        }
        return ResponseEntity.ok(result);
    }
}
