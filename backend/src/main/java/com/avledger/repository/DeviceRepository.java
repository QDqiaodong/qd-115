package com.avledger.repository;

import com.avledger.entity.Device;
import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByDeviceType(DeviceType deviceType);

    List<Device> findByStatus(DeviceStatus status);

    @Query("SELECT DISTINCT d.location FROM Device d WHERE d.location IS NOT NULL AND d.location <> '' ORDER BY d.location")
    List<String> findDistinctLocations();
}
