package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.RepairRecord;
import com.avledger.repository.DeviceRepository;
import com.avledger.repository.RepairRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
}
