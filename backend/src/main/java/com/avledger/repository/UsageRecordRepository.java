package com.avledger.repository;

import com.avledger.entity.UsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface UsageRecordRepository extends JpaRepository<UsageRecord, Long> {

    List<UsageRecord> findByDeviceIdOrderByUsageDateDesc(Long deviceId);

    @Query("SELECT COALESCE(SUM(u.durationMinutes), 0) FROM UsageRecord u WHERE u.device.id = :deviceId")
    Integer sumDurationByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT COALESCE(SUM(u.durationMinutes), 0) FROM UsageRecord u WHERE u.device.id = :deviceId AND u.usageDate >= :startDate")
    Integer sumDurationByDeviceIdAndDateAfter(@Param("deviceId") Long deviceId, @Param("startDate") LocalDate startDate);

    @Query("SELECT COUNT(u) FROM UsageRecord u WHERE u.device.id = :deviceId")
    Long countByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT u.usageDate, COALESCE(SUM(u.durationMinutes), 0) FROM UsageRecord u " +
            "WHERE (:deviceId IS NULL OR u.device.id = :deviceId) " +
            "AND u.usageDate BETWEEN :startDate AND :endDate " +
            "GROUP BY u.usageDate")
    List<Object[]> sumDurationByDateRange(
            @Param("deviceId") Long deviceId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT u FROM UsageRecord u " +
            "WHERE (:deviceId IS NULL OR u.device.id = :deviceId) " +
            "AND u.usageDate = :date " +
            "ORDER BY u.device.name")
    List<UsageRecord> findByDate(
            @Param("deviceId") Long deviceId,
            @Param("date") LocalDate date);
}
