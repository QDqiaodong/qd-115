package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.UsageRecord;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import com.avledger.repository.DeviceRepository;
import com.avledger.repository.UsageRecordRepository;
import com.avledger.util.LocationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private final UsageRecordRepository usageRecordRepository;
    private final DeviceRepository deviceRepository;

    public List<UsageRecord> findByDeviceId(Long deviceId) {
        return usageRecordRepository.findByDeviceIdOrderByUsageDateDesc(deviceId);
    }

    public List<UsageRecord> findAll(Long deviceId) {
        return usageRecordRepository.findAllByDeviceIdOptional(deviceId);
    }

    public Map<String, Object> getUsageStatsByDevice(Long deviceId) {
        Map<String, Object> stats = new HashMap<>();
        Integer total = usageRecordRepository.sumDurationByDeviceId(deviceId);
        Long count = usageRecordRepository.countByDeviceId(deviceId);
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        Integer monthly = usageRecordRepository.sumDurationByDeviceIdAndDateAfter(deviceId, monthStart);

        stats.put("totalMinutes", total != null ? total : 0);
        stats.put("totalHours", total != null ? Math.round(total / 60.0 * 10) / 10.0 : 0.0);
        stats.put("monthlyMinutes", monthly != null ? monthly : 0);
        stats.put("monthlyHours", monthly != null ? Math.round(monthly / 60.0 * 10) / 10.0 : 0.0);
        stats.put("count", count != null ? count : 0L);
        stats.put("avgMinutes", count != null && count > 0 ? Math.round((total != null ? total : 0) * 10.0 / count) / 10.0 : 0.0);
        return stats;
    }

    public Map<String, Object> getUsageStats(Long deviceId) {
        Map<String, Object> stats = new HashMap<>();
        Integer total = usageRecordRepository.sumDurationByDeviceIdOptional(deviceId);
        Long count = usageRecordRepository.countByDeviceIdOptional(deviceId);
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        Integer monthly = usageRecordRepository.sumDurationByDeviceIdAndDateAfterOptional(deviceId, monthStart);

        stats.put("totalMinutes", total != null ? total : 0);
        stats.put("totalHours", total != null ? Math.round(total / 60.0 * 10) / 10.0 : 0.0);
        stats.put("monthlyMinutes", monthly != null ? monthly : 0);
        stats.put("monthlyHours", monthly != null ? Math.round(monthly / 60.0 * 10) / 10.0 : 0.0);
        stats.put("count", count != null ? count : 0L);
        stats.put("avgMinutes", count != null && count > 0 ? Math.round((total != null ? total : 0) * 10.0 / count) / 10.0 : 0.0);
        return stats;
    }

    public Integer getTotalUsageByDevice(Long deviceId) {
        Integer total = usageRecordRepository.sumDurationByDeviceId(deviceId);
        return total != null ? total : 0;
    }

    public List<Map<String, Object>> getHeatmapData(Long deviceId, LocalDate startDate, LocalDate endDate) {
        List<Object[]> rawData = usageRecordRepository.sumDurationByDateRange(deviceId, startDate, endDate);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rawData) {
            Map<String, Object> item = new HashMap<>();
            item.put("date", row[0].toString());
            item.put("minutes", row[1]);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> getDailyDetails(Long deviceId, LocalDate date) {
        List<UsageRecord> records = usageRecordRepository.findByDate(deviceId, date);
        List<Map<String, Object>> result = new ArrayList<>();
        for (UsageRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("scenario", record.getScenario());
            item.put("deviceName", record.getDevice() != null ? record.getDevice().getName() : "未知设备");
            item.put("deviceId", record.getDevice() != null ? record.getDevice().getId() : null);
            item.put("durationMinutes", record.getDurationMinutes());
            item.put("remark", record.getRemark());
            result.add(item);
        }
        return result;
    }

    @Transactional
    public UsageRecord save(UsageRecord usageRecord, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with id: " + deviceId));
        if (device.getStatus() == DeviceStatus.RETIRED) {
            throw new IllegalStateException("退役设备不允许新增使用记录");
        }
        usageRecord.setDevice(device);
        return usageRecordRepository.save(usageRecord);
    }

    @Transactional
    public Optional<UsageRecord> update(Long id, UsageRecord usageRecord, Long deviceId) {
        return usageRecordRepository.findById(id).map(existing -> {
            existing.setUsageDate(usageRecord.getUsageDate());
            existing.setDurationMinutes(usageRecord.getDurationMinutes());
            existing.setScenario(usageRecord.getScenario());
            existing.setRemark(usageRecord.getRemark());
            Long resolvedDeviceId = deviceId;
            if (resolvedDeviceId == null && usageRecord.getDevice() != null && usageRecord.getDevice().getId() != null) {
                resolvedDeviceId = usageRecord.getDevice().getId();
            }
            if (resolvedDeviceId != null) {
                Device device = deviceRepository.findById(resolvedDeviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                if (device.getStatus() == DeviceStatus.RETIRED) {
                    throw new IllegalStateException("退役设备不允许关联使用记录");
                }
                existing.setDevice(device);
            }
            return usageRecordRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (usageRecordRepository.existsById(id)) {
            usageRecordRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Map<String, Object>> getSceneDistribution(DeviceType deviceType, String location) {
        String normalizedLocation = (location != null && !location.isBlank()) 
            ? LocationUtils.normalizeLocation(location) 
            : null;
        List<Object[]> rawData = usageRecordRepository.sumDurationByScene(deviceType, normalizedLocation);
        
        Map<String, Integer> scenarioMap = new HashMap<>();
        int totalMinutes = 0;
        
        if (normalizedLocation != null) {
            List<Device> allDevices = deviceRepository.findAll();
            List<Long> matchingDeviceIds = allDevices.stream()
                .filter(d -> d.getLocation() != null && !d.getLocation().isBlank())
                .filter(d -> LocationUtils.normalizeLocation(d.getLocation()).equals(normalizedLocation))
                .filter(d -> deviceType == null || d.getDeviceType() == deviceType)
                .map(Device::getId)
                .collect(java.util.stream.Collectors.toList());
            
            if (!matchingDeviceIds.isEmpty()) {
                for (Long deviceId : matchingDeviceIds) {
                    List<UsageRecord> records = usageRecordRepository.findByDeviceIdOrderByUsageDateDesc(deviceId);
                    for (UsageRecord record : records) {
                        String scenario = record.getScenario() != null ? record.getScenario() : "未分类";
                        int minutes = record.getDurationMinutes() != null ? record.getDurationMinutes() : 0;
                        scenarioMap.put(scenario, scenarioMap.getOrDefault(scenario, 0) + minutes);
                        totalMinutes += minutes;
                    }
                }
            }
        } else {
            for (Object[] row : rawData) {
                String scenario = row[0] != null ? row[0].toString() : "未分类";
                int minutes = ((Number) row[1]).intValue();
                scenarioMap.put(scenario, minutes);
                totalMinutes += minutes;
            }
        }
        
        List<Map<String, Object>> items = new ArrayList<>();
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(scenarioMap.entrySet());
        sortedEntries.sort((a, b) -> b.getValue() - a.getValue());
        
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            Map<String, Object> item = new HashMap<>();
            item.put("scenario", entry.getKey());
            item.put("durationMinutes", entry.getValue());
            items.add(item);
        }
        
        for (Map<String, Object> item : items) {
            int minutes = (int) item.get("durationMinutes");
            double percent = totalMinutes > 0 ? Math.round(minutes * 1000.0 / totalMinutes) / 10.0 : 0.0;
            item.put("percent", percent);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalMinutes", totalMinutes);
        result.put("items", items);
        List<Map<String, Object>> wrapper = new ArrayList<>();
        wrapper.add(result);
        return wrapper;
    }

    public List<String> getDistinctLocations() {
        List<String> locations = deviceRepository.findDistinctLocations();
        return LocationUtils.getNormalizedLocationList(locations);
    }
}
