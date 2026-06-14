package com.avledger.service;

import com.avledger.entity.Device;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import com.avledger.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Cacheable(value = "devices", key = "'all'")
    public List<Device> findAll() {
        return deviceRepository.findAll();
    }

    public Optional<Device> findById(Long id) {
        return deviceRepository.findById(id);
    }

    @Cacheable(value = "deviceCategories", key = "#deviceType")
    public List<Device> findByDeviceType(DeviceType deviceType) {
        return deviceRepository.findByDeviceType(deviceType);
    }

    public List<Device> findByStatus(DeviceStatus status) {
        return deviceRepository.findByStatus(status);
    }

    @CacheEvict(value = {"devices", "deviceCategories"}, allEntries = true)
    @Transactional
    public Device save(Device device) {
        return deviceRepository.save(device);
    }

    @CacheEvict(value = {"devices", "deviceCategories"}, allEntries = true)
    @Transactional
    public Optional<Device> update(Long id, Device device) {
        return deviceRepository.findById(id).map(existing -> {
            existing.setName(device.getName());
            existing.setModel(device.getModel());
            existing.setDeviceType(device.getDeviceType());
            existing.setPurchaseDate(device.getPurchaseDate());
            existing.setLocation(device.getLocation());
            existing.setHardwareSpecs(device.getHardwareSpecs());
            existing.setStatus(device.getStatus());
            return deviceRepository.save(existing);
        });
    }

    @CacheEvict(value = {"devices", "deviceCategories"}, allEntries = true)
    @Transactional
    public boolean delete(Long id) {
        if (deviceRepository.existsById(id)) {
            deviceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @CacheEvict(value = {"devices", "deviceCategories"}, allEntries = true)
    @Transactional
    public List<Device> batchUpdate(List<Device> devices) {
        return deviceRepository.saveAll(devices);
    }
}
