package com.avledger.repository;

import com.avledger.entity.Device;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByDeviceType(DeviceType deviceType);

    List<Device> findByStatus(DeviceStatus status);
}
