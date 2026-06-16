package com.avledger.service;

import com.avledger.enums.DeviceType;
import com.avledger.enums.MaintenanceType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class MaintenanceIntervalService {

    private static final Map<String, Integer> INTERVAL_DAYS = new LinkedHashMap<>();

    static {
        INTERVAL_DAYS.put(key(DeviceType.SPEAKER, MaintenanceType.CLEANING), 30);
        INTERVAL_DAYS.put(key(DeviceType.SPEAKER, MaintenanceType.CABLE), 90);
        INTERVAL_DAYS.put(key(DeviceType.SPEAKER, MaintenanceType.FIRMWARE), 180);
        INTERVAL_DAYS.put(key(DeviceType.SPEAKER, MaintenanceType.OTHER), 60);

        INTERVAL_DAYS.put(key(DeviceType.PROJECTOR, MaintenanceType.CLEANING), 15);
        INTERVAL_DAYS.put(key(DeviceType.PROJECTOR, MaintenanceType.CABLE), 60);
        INTERVAL_DAYS.put(key(DeviceType.PROJECTOR, MaintenanceType.FIRMWARE), 90);
        INTERVAL_DAYS.put(key(DeviceType.PROJECTOR, MaintenanceType.OTHER), 45);

        INTERVAL_DAYS.put(key(DeviceType.PLAYER, MaintenanceType.CLEANING), 30);
        INTERVAL_DAYS.put(key(DeviceType.PLAYER, MaintenanceType.CABLE), 90);
        INTERVAL_DAYS.put(key(DeviceType.PLAYER, MaintenanceType.FIRMWARE), 120);
        INTERVAL_DAYS.put(key(DeviceType.PLAYER, MaintenanceType.OTHER), 60);

        INTERVAL_DAYS.put(key(DeviceType.AMPLIFIER, MaintenanceType.CLEANING), 30);
        INTERVAL_DAYS.put(key(DeviceType.AMPLIFIER, MaintenanceType.CABLE), 60);
        INTERVAL_DAYS.put(key(DeviceType.AMPLIFIER, MaintenanceType.FIRMWARE), 180);
        INTERVAL_DAYS.put(key(DeviceType.AMPLIFIER, MaintenanceType.OTHER), 60);
    }

    private static String key(DeviceType deviceType, MaintenanceType maintenanceType) {
        return deviceType.name() + "_" + maintenanceType.name();
    }

    public int getIntervalDays(DeviceType deviceType, MaintenanceType maintenanceType) {
        return INTERVAL_DAYS.getOrDefault(key(deviceType, maintenanceType), 90);
    }

    public LocalDateTime calculateNextWindow(DeviceType deviceType, LocalDateTime lastMaintenanceTime, MaintenanceType maintenanceType) {
        if (lastMaintenanceTime == null) {
            return null;
        }
        int days = getIntervalDays(deviceType, maintenanceType);
        return lastMaintenanceTime.plusDays(days);
    }

    public Map<String, Object> calculateNextWindows(DeviceType deviceType, LocalDateTime lastMaintenanceTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (MaintenanceType type : MaintenanceType.values()) {
            int interval = getIntervalDays(deviceType, type);
            LocalDateTime nextTime = calculateNextWindow(deviceType, lastMaintenanceTime, type);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("maintenanceType", type.name());
            entry.put("intervalDays", interval);
            entry.put("nextTime", nextTime != null ? nextTime.toString() : null);
            if (nextTime != null) {
                entry.put("overdue", LocalDateTime.now().isAfter(nextTime));
                entry.put("daysUntil", java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), nextTime));
            }
            result.put(type.name(), entry);
        }
        return result;
    }

    public Map<String, Integer> getAllIntervals() {
        Map<String, Integer> all = new LinkedHashMap<>();
        INTERVAL_DAYS.forEach((k, v) -> all.put(k, v));
        return all;
    }

    public Map<String, Integer> getIntervalsByDeviceType(DeviceType deviceType) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (MaintenanceType type : MaintenanceType.values()) {
            result.put(type.name(), getIntervalDays(deviceType, type));
        }
        return result;
    }
}
