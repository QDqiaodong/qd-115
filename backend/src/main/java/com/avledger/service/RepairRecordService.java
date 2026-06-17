package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.RepairRecord;
import com.avledger.enums.DeviceType;
import com.avledger.repository.DeviceRepository;
import com.avledger.repository.RepairRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepairRecordService {

    private final RepairRecordRepository repairRecordRepository;
    private final DeviceRepository deviceRepository;

    public List<RepairRecord> findByDeviceId(Long deviceId) {
        return repairRecordRepository.findByDeviceIdOrderByRepairTimeDesc(deviceId);
    }

    public List<RepairRecord> getRecentRepairs() {
        return repairRecordRepository.findTop10ByOrderByRepairTimeDesc();
    }

    @Transactional
    public RepairRecord save(RepairRecord repairRecord, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with id: " + deviceId));
        repairRecord.setDevice(device);
        return repairRecordRepository.save(repairRecord);
    }

    @Transactional
    public Optional<RepairRecord> update(Long id, RepairRecord repairRecord) {
        return repairRecordRepository.findById(id).map(existing -> {
            existing.setRepairTime(repairRecord.getRepairTime());
            existing.setSymptom(repairRecord.getSymptom());
            existing.setCause(repairRecord.getCause());
            existing.setFixMethod(repairRecord.getFixMethod());
            existing.setRepairPerson(repairRecord.getRepairPerson());
            existing.setCost(repairRecord.getCost());
            existing.setRemark(repairRecord.getRemark());
            if (repairRecord.getDevice() != null && repairRecord.getDevice().getId() != null) {
                Device device = deviceRepository.findById(repairRecord.getDevice().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                existing.setDevice(device);
            }
            return repairRecordRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (repairRecordRepository.existsById(id)) {
            repairRecordRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Map<String, Object>> getRepairTimeline(Long deviceId) {
        List<RepairRecord> records = repairRecordRepository.findByDeviceIdOrderByRepairTimeAsc(deviceId);
        Map<String, Long> symptomCounts = records.stream()
                .collect(Collectors.groupingBy(RepairRecord::getSymptom, Collectors.counting()));
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (RepairRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("repairTime", r.getRepairTime());
            item.put("symptom", r.getSymptom());
            item.put("cause", r.getCause());
            item.put("fixMethod", r.getFixMethod());
            item.put("repairPerson", r.getRepairPerson());
            item.put("cost", r.getCost());
            item.put("remark", r.getRemark());
            item.put("repeatedFault", symptomCounts.getOrDefault(r.getSymptom(), 1L) > 1);
            timeline.add(item);
        }
        return timeline;
    }

    public Map<String, Object> getCostStatistics(DeviceType deviceType, String location, String yearMonth, String cause) {
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (yearMonth != null && !yearMonth.isBlank()) {
            YearMonth ym = YearMonth.parse(yearMonth);
            startTime = ym.atDay(1).atStartOfDay();
            endTime = ym.atEndOfMonth().atTime(23, 59, 59);
        }
        List<RepairRecord> records = repairRecordRepository.findByFilters(deviceType, location, startTime, endTime, cause);

        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal totalCost = records.stream()
                .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalCount = records.size();
        BigDecimal avgCost = totalCount > 0
                ? totalCost.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        result.put("totalCost", totalCost);
        result.put("totalCount", totalCount);
        result.put("avgCost", avgCost);

        Map<String, Map<String, Object>> byDeviceType = new LinkedHashMap<>();
        for (DeviceType dt : DeviceType.values()) {
            List<RepairRecord> filtered = records.stream()
                    .filter(r -> r.getDevice().getDeviceType() == dt)
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) {
                Map<String, Object> stats = new LinkedHashMap<>();
                BigDecimal cost = filtered.stream()
                        .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                stats.put("totalCost", cost);
                stats.put("count", filtered.size());
                stats.put("avgCost", cost.divide(BigDecimal.valueOf(filtered.size()), 2, RoundingMode.HALF_UP));
                byDeviceType.put(dt.name(), stats);
            }
        }
        result.put("byDeviceType", byDeviceType);

        Map<String, Map<String, Object>> byLocation = new LinkedHashMap<>();
        records.stream()
                .collect(Collectors.groupingBy(r -> r.getDevice().getLocation() != null ? r.getDevice().getLocation() : "未知"))
                .forEach((loc, recs) -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    BigDecimal cost = recs.stream()
                            .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    stats.put("totalCost", cost);
                    stats.put("count", recs.size());
                    stats.put("avgCost", cost.divide(BigDecimal.valueOf(recs.size()), 2, RoundingMode.HALF_UP));
                    byLocation.put(loc, stats);
                });
        result.put("byLocation", byLocation);

        Map<String, Map<String, Object>> byMonth = new LinkedHashMap<>();
        records.stream()
                .collect(Collectors.groupingBy(r -> r.getRepairTime().getYear() + "-" +
                        String.format("%02d", r.getRepairTime().getMonthValue())))
                .forEach((month, recs) -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    BigDecimal cost = recs.stream()
                            .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    stats.put("totalCost", cost);
                    stats.put("count", recs.size());
                    stats.put("avgCost", cost.divide(BigDecimal.valueOf(recs.size()), 2, RoundingMode.HALF_UP));
                    byMonth.put(month, stats);
                });
        result.put("byMonth", byMonth);

        Map<String, Map<String, Object>> byCause = new LinkedHashMap<>();
        records.stream()
                .filter(r -> r.getCause() != null && !r.getCause().isBlank())
                .collect(Collectors.groupingBy(RepairRecord::getCause))
                .forEach((c, recs) -> {
                    Map<String, Object> stats = new LinkedHashMap<>();
                    BigDecimal cost = recs.stream()
                            .map(r -> r.getCost() != null ? r.getCost() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    stats.put("totalCost", cost);
                    stats.put("count", recs.size());
                    stats.put("avgCost", cost.divide(BigDecimal.valueOf(recs.size()), 2, RoundingMode.HALF_UP));
                    byCause.put(c, stats);
                });
        result.put("byCause", byCause);

        result.put("records", records);
        return result;
    }
}
