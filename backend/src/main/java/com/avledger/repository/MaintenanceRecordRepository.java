package com.avledger.repository;

import com.avledger.entity.MaintenanceRecord;
import com.avledger.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByDeviceIdOrderByMaintenanceTimeDesc(Long deviceId);

    List<MaintenanceRecord> findByMaintenanceType(MaintenanceType maintenanceType);

    Optional<MaintenanceRecord> findTopByDeviceIdOrderByMaintenanceTimeDesc(Long deviceId);
}
