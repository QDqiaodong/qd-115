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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    private static final Map<DeviceStatus, List<DeviceStatus>> TRANSITION_RULES = Map.of(
            DeviceStatus.NORMAL, Arrays.asList(DeviceStatus.FAULTY, DeviceStatus.MAINTENANCE, DeviceStatus.RETIRED),
            DeviceStatus.FAULTY, Arrays.asList(DeviceStatus.NORMAL, DeviceStatus.MAINTENANCE, DeviceStatus.RETIRED),
            DeviceStatus.MAINTENANCE, Arrays.asList(DeviceStatus.NORMAL, DeviceStatus.FAULTY, DeviceStatus.RETIRED),
            DeviceStatus.RETIRED, List.of()
    );

    public List<DeviceStatus> getAllowedTransitions(DeviceStatus current) {
        return TRANSITION_RULES.getOrDefault(current, List.of());
    }

    public boolean isTransitionAllowed(DeviceStatus from, DeviceStatus to) {
        if (from == to) return true;
        return TRANSITION_RULES.getOrDefault(from, List.of()).contains(to);
    }

    private void validateTransition(DeviceStatus from, DeviceStatus to) {
        if (!isTransitionAllowed(from, to)) {
            throw new IllegalStateException(
                    "设备状态不允许从 " + from.name() + " 转换为 " + to.name() +
                    "，退役设备不可恢复为使用或保养状态");
        }
    }

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
            if (device.getStatus() != null && device.getStatus() != existing.getStatus()) {
                validateTransition(existing.getStatus(), device.getStatus());
            }
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
        List<Device> result = new ArrayList<>();
        for (Device device : devices) {
            if (device.getId() == null) continue;
            Device existing = deviceRepository.findById(device.getId()).orElse(null);
            if (existing == null) continue;

            if (device.getStatus() != null && device.getStatus() != existing.getStatus()) {
                validateTransition(existing.getStatus(), device.getStatus());
                existing.setStatus(device.getStatus());
            }
            if (device.getName() != null) {
                existing.setName(device.getName());
            }
            if (device.getModel() != null) {
                existing.setModel(device.getModel());
            }
            if (device.getDeviceType() != null) {
                existing.setDeviceType(device.getDeviceType());
            }
            if (device.getPurchaseDate() != null) {
                existing.setPurchaseDate(device.getPurchaseDate());
            }
            if (device.getLocation() != null) {
                existing.setLocation(device.getLocation());
            }
            if (device.getHardwareSpecs() != null) {
                existing.setHardwareSpecs(device.getHardwareSpecs());
            }
            result.add(deviceRepository.save(existing));
        }
        return result;
    }
}
