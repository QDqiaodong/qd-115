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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
        DeviceStatus status = device.getStatus();
        if (status == DeviceStatus.RETIRED || status == DeviceStatus.FAULTY) {
            throw new IllegalArgumentException(
                    status == DeviceStatus.RETIRED ? "退役设备不允许新增使用记录" : "故障设备不允许新增使用记录");
        }
        if (status == DeviceStatus.MAINTENANCE) {
            throw new IllegalStateException("设备正在保养中，暂时无法新增使用记录，请待保养完成后再操作");
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
                DeviceStatus status = device.getStatus();
                if (status == DeviceStatus.RETIRED || status == DeviceStatus.FAULTY) {
                    throw new IllegalArgumentException(
                            status == DeviceStatus.RETIRED ? "退役设备不允许关联使用记录" : "故障设备不允许关联使用记录");
                }
                if (status == DeviceStatus.MAINTENANCE) {
                    throw new IllegalStateException("设备正在保养中，暂时无法关联使用记录，请待保养完成后再操作");
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

    public Map<String, Object> getDeviceUsageSummary(DeviceType deviceType, String location) {
        String normalizedLocation = (location != null && !location.isBlank()) 
            ? LocationUtils.normalizeLocation(location) 
            : null;
        
        Map<Long, Map<String, Object>> deviceSummaryMap = new java.util.LinkedHashMap<>();
        Map<String, Map<String, Object>> deviceTypeSummaryMap = new java.util.LinkedHashMap<>();
        Map<String, Map<String, Object>> locationSummaryMap = new java.util.LinkedHashMap<>();
        
        List<Device> allDevices = deviceRepository.findAll();
        
        for (Device device : allDevices) {
            if (deviceType != null && device.getDeviceType() != deviceType) {
                continue;
            }
            if (normalizedLocation != null) {
                String deviceLocation = device.getLocation();
                if (deviceLocation == null || deviceLocation.isBlank()) {
                    continue;
                }
                String deviceNormalizedLocation = LocationUtils.normalizeLocation(deviceLocation);
                if (!deviceNormalizedLocation.equals(normalizedLocation)) {
                    continue;
                }
            }
            
            List<UsageRecord> records = usageRecordRepository.findByDeviceIdOrderByUsageDateDesc(device.getId());
            if (records.isEmpty()) {
                continue;
            }
            
            int totalMinutes = 0;
            LocalDate lastUsedDate = null;
            int maxSingleDuration = 0;
            
            for (UsageRecord record : records) {
                int duration = record.getDurationMinutes() != null ? record.getDurationMinutes() : 0;
                totalMinutes += duration;
                if (lastUsedDate == null || record.getUsageDate().isAfter(lastUsedDate)) {
                    lastUsedDate = record.getUsageDate();
                }
                if (duration > maxSingleDuration) {
                    maxSingleDuration = duration;
                }
            }
            
            Map<String, Object> deviceSummary = new HashMap<>();
            deviceSummary.put("deviceId", device.getId());
            deviceSummary.put("deviceName", device.getName());
            deviceSummary.put("deviceType", device.getDeviceType().name());
            deviceSummary.put("deviceTypeLabel", device.getDeviceType().getLabel());
            deviceSummary.put("location", device.getLocation());
            deviceSummary.put("normalizedLocation", LocationUtils.normalizeLocation(device.getLocation()));
            deviceSummary.put("totalMinutes", totalMinutes);
            deviceSummary.put("totalHours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
            deviceSummary.put("lastUsedDate", lastUsedDate != null ? lastUsedDate.toString() : null);
            deviceSummary.put("maxSingleDuration", maxSingleDuration);
            deviceSummary.put("maxSingleDurationHours", Math.round(maxSingleDuration / 60.0 * 10) / 10.0);
            
            deviceSummaryMap.put(device.getId(), deviceSummary);
            
            String typeKey = device.getDeviceType().name();
            Map<String, Object> typeSummary = deviceTypeSummaryMap.get(typeKey);
            if (typeSummary == null) {
                typeSummary = new HashMap<>();
                typeSummary.put("deviceType", device.getDeviceType().name());
                typeSummary.put("deviceTypeLabel", device.getDeviceType().getLabel());
                typeSummary.put("totalMinutes", 0);
                typeSummary.put("lastUsedDate", null);
                typeSummary.put("maxSingleDuration", 0);
                deviceTypeSummaryMap.put(typeKey, typeSummary);
            }
            int typeTotalMinutes = (int) typeSummary.get("totalMinutes") + totalMinutes;
            typeSummary.put("totalMinutes", typeTotalMinutes);
            typeSummary.put("totalHours", Math.round(typeTotalMinutes / 60.0 * 10) / 10.0);
            String typeLastUsed = (String) typeSummary.get("lastUsedDate");
            if (typeLastUsed == null || (lastUsedDate != null && lastUsedDate.isAfter(LocalDate.parse(typeLastUsed)))) {
                typeSummary.put("lastUsedDate", lastUsedDate != null ? lastUsedDate.toString() : null);
            }
            int typeMaxDuration = Math.max((int) typeSummary.get("maxSingleDuration"), maxSingleDuration);
            typeSummary.put("maxSingleDuration", typeMaxDuration);
            typeSummary.put("maxSingleDurationHours", Math.round(typeMaxDuration / 60.0 * 10) / 10.0);
            
            String locKey = LocationUtils.normalizeLocation(device.getLocation());
            if (locKey != null && !locKey.isEmpty()) {
                Map<String, Object> locSummary = locationSummaryMap.get(locKey);
                if (locSummary == null) {
                    locSummary = new HashMap<>();
                    locSummary.put("location", locKey);
                    locSummary.put("totalMinutes", 0);
                    locSummary.put("lastUsedDate", null);
                    locSummary.put("maxSingleDuration", 0);
                    locationSummaryMap.put(locKey, locSummary);
                }
                int locTotalMinutes = (int) locSummary.get("totalMinutes") + totalMinutes;
                locSummary.put("totalMinutes", locTotalMinutes);
                locSummary.put("totalHours", Math.round(locTotalMinutes / 60.0 * 10) / 10.0);
                String locLastUsed = (String) locSummary.get("lastUsedDate");
                if (locLastUsed == null || (lastUsedDate != null && lastUsedDate.isAfter(LocalDate.parse(locLastUsed)))) {
                    locSummary.put("lastUsedDate", lastUsedDate != null ? lastUsedDate.toString() : null);
                }
                int locMaxDuration = Math.max((int) locSummary.get("maxSingleDuration"), maxSingleDuration);
                locSummary.put("maxSingleDuration", locMaxDuration);
                locSummary.put("maxSingleDurationHours", Math.round(locMaxDuration / 60.0 * 10) / 10.0);
            }
        }
        
        List<Map<String, Object>> byDevice = new ArrayList<>(deviceSummaryMap.values());
        byDevice.sort((a, b) -> (int) b.get("totalMinutes") - (int) a.get("totalMinutes"));
        
        List<Map<String, Object>> byDeviceType = new ArrayList<>(deviceTypeSummaryMap.values());
        byDeviceType.sort((a, b) -> (int) b.get("totalMinutes") - (int) a.get("totalMinutes"));
        
        List<Map<String, Object>> byLocation = new ArrayList<>(locationSummaryMap.values());
        byLocation.sort((a, b) -> (int) b.get("totalMinutes") - (int) a.get("totalMinutes"));
        byLocation = LocationUtils.sortLocations(byLocation.stream()
                .map(m -> (String) m.get("location"))
                .collect(Collectors.toList())).stream()
                .map(loc -> byLocation.stream()
                        .filter(m -> loc.equals(m.get("location")))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("byDevice", byDevice);
        result.put("byDeviceType", byDeviceType);
        result.put("byLocation", byLocation);
        
        return result;
    }
}
