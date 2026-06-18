package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.MaintenanceRecord;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.MaintenanceType;
import com.avledger.repository.DeviceRepository;
import com.avledger.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    public List<MaintenanceRecord> findAll(Long deviceId) {
        return maintenanceRecordRepository.findAllByDeviceIdOptional(deviceId);
    }

    @Cacheable(value = "maintenanceCycles", key = "#type")
    public List<MaintenanceRecord> findByType(MaintenanceType type) {
        return maintenanceRecordRepository.findByMaintenanceType(type);
    }

    @CacheEvict(value = "maintenanceCycles", allEntries = true)
    @Transactional
    public MaintenanceRecord save(MaintenanceRecord maintenanceRecord, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with id: " + deviceId));
        if (device.getStatus() == DeviceStatus.RETIRED) {
            throw new IllegalStateException("退役设备不允许新增保养记录");
        }
        maintenanceRecord.setDevice(device);
        return maintenanceRecordRepository.save(maintenanceRecord);
    }

    @CacheEvict(value = "maintenanceCycles", allEntries = true)
    @Transactional
    public Optional<MaintenanceRecord> update(Long id, MaintenanceRecord maintenanceRecord, Long deviceId) {
        return maintenanceRecordRepository.findById(id).map(existing -> {
            existing.setMaintenanceTime(maintenanceRecord.getMaintenanceTime());
            existing.setMaintenanceType(maintenanceRecord.getMaintenanceType());
            existing.setContent(maintenanceRecord.getContent());
            existing.setOperator(maintenanceRecord.getOperator());
            existing.setRemark(maintenanceRecord.getRemark());
            Long resolvedDeviceId = deviceId;
            if (resolvedDeviceId == null && maintenanceRecord.getDevice() != null && maintenanceRecord.getDevice().getId() != null) {
                resolvedDeviceId = maintenanceRecord.getDevice().getId();
            }
            if (resolvedDeviceId != null) {
                Device device = deviceRepository.findById(resolvedDeviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                if (device.getStatus() == DeviceStatus.RETIRED) {
                    throw new IllegalStateException("退役设备不允许关联保养记录");
                }
                existing.setDevice(device);
            } else {
                if (existing.getDevice() != null && existing.getDevice().getStatus() == DeviceStatus.RETIRED) {
                    throw new IllegalStateException("退役设备不允许关联保养记录");
                }
            }
            return maintenanceRecordRepository.save(existing);
        });
    }

    @CacheEvict(value = "maintenanceCycles", allEntries = true)
    @Transactional
    public boolean delete(Long id) {
        if (maintenanceRecordRepository.existsById(id)) {
            maintenanceRecordRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
