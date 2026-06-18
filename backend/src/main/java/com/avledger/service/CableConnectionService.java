package com.avledger.service;

import com.avledger.entity.CableConnection;
import com.avledger.entity.Device;
import com.avledger.repository.CableConnectionRepository;
import com.avledger.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CableConnectionService {

    private final CableConnectionRepository cableConnectionRepository;
    private final DeviceRepository deviceRepository;

    public List<CableConnection> findAll(Long deviceId) {
        return cableConnectionRepository.findAllByDeviceIdOptional(deviceId);
    }

    public List<CableConnection> findByDeviceId(Long deviceId) {
        return cableConnectionRepository.findByDeviceId(deviceId);
    }

    public Optional<CableConnection> findById(Long id) {
        return cableConnectionRepository.findById(id);
    }

    @Transactional
    public CableConnection save(CableConnection cableConnection, Long sourceDeviceId, Long targetDeviceId) {
        Device sourceDevice = deviceRepository.findById(sourceDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("源设备不存在，id: " + sourceDeviceId));
        Device targetDevice = deviceRepository.findById(targetDeviceId)
                .orElseThrow(() -> new IllegalArgumentException("目标设备不存在，id: " + targetDeviceId));
        if (sourceDeviceId.equals(targetDeviceId)) {
            throw new IllegalArgumentException("源设备和目标设备不能相同");
        }
        cableConnection.setSourceDevice(sourceDevice);
        cableConnection.setTargetDevice(targetDevice);
        return cableConnectionRepository.save(cableConnection);
    }

    @Transactional
    public Optional<CableConnection> update(Long id, CableConnection cableConnection, Long sourceDeviceId, Long targetDeviceId) {
        return cableConnectionRepository.findById(id).map(existing -> {
            existing.setInterfaceType(cableConnection.getInterfaceType());
            existing.setCableLength(cableConnection.getCableLength());
            existing.setConnectionLocation(cableConnection.getConnectionLocation());
            existing.setSourcePort(cableConnection.getSourcePort());
            existing.setTargetPort(cableConnection.getTargetPort());
            existing.setRemark(cableConnection.getRemark());

            if (sourceDeviceId != null && targetDeviceId != null && sourceDeviceId.equals(targetDeviceId)) {
                throw new IllegalArgumentException("源设备和目标设备不能相同");
            }

            if (sourceDeviceId != null) {
                Device sourceDevice = deviceRepository.findById(sourceDeviceId)
                        .orElseThrow(() -> new IllegalArgumentException("源设备不存在"));
                existing.setSourceDevice(sourceDevice);
            }
            if (targetDeviceId != null) {
                Device targetDevice = deviceRepository.findById(targetDeviceId)
                        .orElseThrow(() -> new IllegalArgumentException("目标设备不存在"));
                existing.setTargetDevice(targetDevice);
            }

            if (existing.getSourceDevice() != null && existing.getTargetDevice() != null
                    && existing.getSourceDevice().getId().equals(existing.getTargetDevice().getId())) {
                throw new IllegalArgumentException("源设备和目标设备不能相同");
            }

            return cableConnectionRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (cableConnectionRepository.existsById(id)) {
            cableConnectionRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
