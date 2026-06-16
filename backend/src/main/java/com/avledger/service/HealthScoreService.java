package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.MaintenanceRecord;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import com.avledger.repository.MaintenanceRecordRepository;
import com.avledger.repository.RepairRecordRepository;
import com.avledger.repository.UsageRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthScoreService {

    private final RepairRecordRepository repairRecordRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final MaintenanceIntervalService maintenanceIntervalService;

    private static final double WEIGHT_STATUS = 0.30;
    private static final double WEIGHT_REPAIR = 0.25;
    private static final double WEIGHT_MAINTENANCE = 0.25;
    private static final double WEIGHT_USAGE = 0.20;

    public Map<String, Object> calculateHealthScore(Device device) {
        Map<String, Object> result = new LinkedHashMap<>();

        int statusScore = calculateStatusScore(device.getStatus());
        int repairScore = calculateRepairScore(device.getId());
        int maintenanceScore = calculateMaintenanceScore(device);
        int usageScore = calculateUsageScore(device);

        double totalScore = statusScore * WEIGHT_STATUS
                + repairScore * WEIGHT_REPAIR
                + maintenanceScore * WEIGHT_MAINTENANCE
                + usageScore * WEIGHT_USAGE;
        int finalScore = (int) Math.round(totalScore);

        result.put("score", finalScore);
        result.put("level", scoreLevel(finalScore));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("statusScore", statusScore);
        details.put("repairScore", repairScore);
        details.put("maintenanceScore", maintenanceScore);
        details.put("usageScore", usageScore);

        details.put("statusDetail", statusDetail(device.getStatus()));
        details.put("repairDetail", repairDetail(device.getId()));
        details.put("maintenanceDetail", maintenanceDetail(device));
        details.put("usageDetail", usageDetail(device));

        result.put("details", details);
        return result;
    }

    private int calculateStatusScore(DeviceStatus status) {
        return switch (status) {
            case NORMAL -> 100;
            case MAINTENANCE -> 70;
            case FAULTY -> 40;
            case RETIRED -> 0;
        };
    }

    private Map<String, Object> statusDetail(DeviceStatus status) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("status", status.name());
        return d;
    }

    private int calculateRepairScore(Long deviceId) {
        long count = repairRecordRepository.countByDeviceId(deviceId);
        if (count == 0) return 100;
        if (count == 1) return 80;
        if (count == 2) return 60;
        if (count == 3) return 40;
        return 20;
    }

    private Map<String, Object> repairDetail(Long deviceId) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("repairCount", repairRecordRepository.countByDeviceId(deviceId));
        return d;
    }

    private int calculateMaintenanceScore(Device device) {
        MaintenanceRecord lastMaintenance = maintenanceRecordRepository
                .findTopByDeviceIdOrderByMaintenanceTimeDesc(device.getId())
                .orElse(null);

        if (lastMaintenance == null) {
            return 30;
        }

        Map<String, Object> windows = maintenanceIntervalService.calculateNextWindows(
                device.getDeviceType(),
                lastMaintenance.getMaintenanceTime());

        int worstOverdueDays = 0;
        for (Map.Entry<String, Object> entry : windows.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> w) {
                Object overdueObj = w.get("overdue");
                Object daysUntilObj = w.get("daysUntil");
                boolean overdue = Boolean.TRUE.equals(overdueObj);
                if (overdue && daysUntilObj instanceof Number n) {
                    int overdueDays = Math.abs(n.intValue());
                    worstOverdueDays = Math.max(worstOverdueDays, overdueDays);
                }
            }
        }

        if (worstOverdueDays == 0) return 100;
        if (worstOverdueDays <= 7) return 80;
        if (worstOverdueDays <= 30) return 50;
        return 20;
    }

    private Map<String, Object> maintenanceDetail(Device device) {
        Map<String, Object> d = new LinkedHashMap<>();
        maintenanceRecordRepository.findTopByDeviceIdOrderByMaintenanceTimeDesc(device.getId())
                .ifPresent(m -> d.put("lastMaintenanceTime", m.getMaintenanceTime().toString()));
        return d;
    }

    private int calculateUsageScore(Device device) {
        if (device.getPurchaseDate() == null) return 80;

        long totalMinutes = usageRecordRepository.sumDurationByDeviceId(device.getId());
        long daysOwned = ChronoUnit.DAYS.between(device.getPurchaseDate(), LocalDate.now());
        if (daysOwned <= 0) daysOwned = 1;

        double avgHoursPerDay = (totalMinutes / 60.0) / daysOwned;

        if (avgHoursPerDay < 4) return 100;
        if (avgHoursPerDay < 8) return 80;
        if (avgHoursPerDay < 12) return 60;
        if (avgHoursPerDay < 16) return 40;
        return 20;
    }

    private Map<String, Object> usageDetail(Device device) {
        Map<String, Object> d = new LinkedHashMap<>();
        long totalMinutes = usageRecordRepository.sumDurationByDeviceId(device.getId());
        d.put("totalUsageMinutes", totalMinutes);
        if (device.getPurchaseDate() != null) {
            long daysOwned = ChronoUnit.DAYS.between(device.getPurchaseDate(), LocalDate.now());
            if (daysOwned <= 0) daysOwned = 1;
            d.put("daysOwned", daysOwned);
            d.put("avgHoursPerDay", Math.round(totalMinutes / 60.0 / daysOwned * 100) / 100.0);
        }
        return d;
    }

    private String scoreLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "较差";
        return "危险";
    }
}
