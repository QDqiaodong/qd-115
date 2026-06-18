package com.avledger.repository;

import com.avledger.entity.MaintenanceRecord;
import com.avledger.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    List<MaintenanceRecord> findByDeviceIdOrderByMaintenanceTimeDesc(Long deviceId);

    @Query("SELECT m FROM MaintenanceRecord m WHERE (:deviceId IS NULL OR m.device.id = :deviceId) ORDER BY m.maintenanceTime DESC")
    List<MaintenanceRecord> findAllByDeviceIdOptional(@Param("deviceId") Long deviceId);

    List<MaintenanceRecord> findByMaintenanceType(MaintenanceType maintenanceType);

    Optional<MaintenanceRecord> findTopByDeviceIdOrderByMaintenanceTimeDesc(Long deviceId);

    @Query("SELECT m FROM MaintenanceRecord m WHERE m.device.id = :deviceId " +
           "AND m.maintenanceType = :maintenanceType " +
           "AND FUNCTION('DATE', m.maintenanceTime) = :maintenanceDate " +
           "AND (:excludeId IS NULL OR m.id != :excludeId) " +
           "ORDER BY m.maintenanceTime DESC")
    List<MaintenanceRecord> findDuplicateRecords(
            @Param("deviceId") Long deviceId,
            @Param("maintenanceType") MaintenanceType maintenanceType,
            @Param("maintenanceDate") LocalDate maintenanceDate,
            @Param("excludeId") Long excludeId
    );
}
