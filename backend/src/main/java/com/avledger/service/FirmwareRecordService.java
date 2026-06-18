package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.entity.FirmwareRecord;
import com.avledger.enums.DeviceStatus;
import com.avledger.repository.DeviceRepository;
import com.avledger.repository.FirmwareRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FirmwareRecordService {

    private final FirmwareRecordRepository firmwareRecordRepository;
    private final DeviceRepository deviceRepository;

    public List<FirmwareRecord> findByDeviceId(Long deviceId) {
        return firmwareRecordRepository.findByDeviceIdOrderByUpdateTimeDesc(deviceId);
    }

    public Optional<FirmwareRecord> findLatestByDeviceId(Long deviceId) {
        return firmwareRecordRepository.findTopByDeviceIdOrderByUpdateTimeDesc(deviceId);
    }

    public Optional<FirmwareRecord> findById(Long id) {
        return firmwareRecordRepository.findById(id);
    }

    @Transactional
    public FirmwareRecord save(FirmwareRecord firmwareRecord, Long deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found with id: " + deviceId));
        if (device.getStatus() == DeviceStatus.RETIRED) {
            throw new IllegalStateException("退役设备不允许新增固件记录");
        }
        firmwareRecord.setDevice(device);
        return firmwareRecordRepository.save(firmwareRecord);
    }

    @Transactional
    public Optional<FirmwareRecord> update(Long id, FirmwareRecord firmwareRecord) {
        return firmwareRecordRepository.findById(id).map(existing -> {
            existing.setFirmwareVersion(firmwareRecord.getFirmwareVersion());
            existing.setUpdateTime(firmwareRecord.getUpdateTime());
            existing.setDescription(firmwareRecord.getDescription());
            existing.setOperator(firmwareRecord.getOperator());
            existing.setRemark(firmwareRecord.getRemark());
            if (firmwareRecord.getDevice() != null && firmwareRecord.getDevice().getId() != null) {
                Device device = deviceRepository.findById(firmwareRecord.getDevice().getId())
                        .orElseThrow(() -> new IllegalArgumentException("Device not found"));
                if (device.getStatus() == DeviceStatus.RETIRED) {
                    throw new IllegalStateException("退役设备不允许关联固件记录");
                }
                existing.setDevice(device);
            } else {
                if (existing.getDevice() != null && existing.getDevice().getStatus() == DeviceStatus.RETIRED) {
                    throw new IllegalStateException("退役设备不允许关联固件记录");
                }
            }
            return firmwareRecordRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (firmwareRecordRepository.existsById(id)) {
            firmwareRecordRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
