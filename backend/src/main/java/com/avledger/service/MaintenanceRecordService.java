package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.MaintenanceRecord;
import com.avledger.enums.MaintenanceType;
import com.avledger.repository.DeviceRepository;
import com.avledger.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MaintenanceRecordService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final DeviceRepository deviceRepository;

    public List<MaintenanceRecord> findByDeviceId(Long deviceId) {
        return maintenanceRecordRepository.findByDeviceIdOrderByMaintenanceTimeDesc(deviceId);
    }

    @Cacheable(value = "maintenanceCycles", key = "#type")
    public List<MaintenanceRecord> findByType(MaintenanceType type) {
        return maintenanceRecordRepository.findByMaintenanceType(type);
    }

    @Transactional
    public MaintenanceRecord save(MaintenanceRecord maintenanceRecord, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with id: " + deviceId));
        maintenanceRecord.setDevice(device);
        return maintenanceRecordRepository.save(maintenanceRecord);
    }

    @Transactional
    public Optional<MaintenanceRecord> update(Long id, MaintenanceRecord maintenanceRecord) {
        return maintenanceRecordRepository.findById(id).map(existing -> {
            existing.setMaintenanceTime(maintenanceRecord.getMaintenanceTime());
            existing.setMaintenanceType(maintenanceRecord.getMaintenanceType());
            existing.setContent(maintenanceRecord.getContent());
            existing.setOperator(maintenanceRecord.getOperator());
            existing.setRemark(maintenanceRecord.getRemark());
            if (maintenanceRecord.getDevice() != null && maintenanceRecord.getDevice().getId() != null) {
                Device device = deviceRepository.findById(maintenanceRecord.getDevice().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                existing.setDevice(device);
            }
            return maintenanceRecordRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (maintenanceRecordRepository.existsById(id)) {
            maintenanceRecordRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
