package com.avledger.repository;

import com.avledger.entity.RepairRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {

    List<RepairRecord> findByDeviceIdOrderByRepairTimeDesc(Long deviceId);

    List<RepairRecord> findTop10ByOrderByRepairTimeDesc();

    Optional<RepairRecord> findTopByDeviceIdOrderByRepairTimeDesc(Long deviceId);

    long countByDeviceId(Long deviceId);

    List<RepairRecord> findByDeviceIdOrderByRepairTimeAsc(Long deviceId);

    @Query("SELECT r FROM RepairRecord r " +
           "JOIN r.device d " +
           "WHERE (:deviceType IS NULL OR d.deviceType = :deviceType) " +
           "AND (:location IS NULL OR d.location = :location) " +
           "AND (:startTime IS NULL OR r.repairTime >= :startTime) " +
           "AND (:endTime IS NULL OR r.repairTime <= :endTime) " +
           "AND (:cause IS NULL OR r.cause LIKE CONCAT('%', :cause, '%')) " +
           "ORDER BY r.repairTime DESC")
    List<RepairRecord> findByFilters(
            @Param("deviceType") com.avledger.enums.DeviceType deviceType,
            @Param("location") String location,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("cause") String cause
    );
}
