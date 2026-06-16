package com.avledger.repository;

import com.avledger.entity.FirmwareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FirmwareRecordRepository extends JpaRepository<FirmwareRecord, Long> {

    List<FirmwareRecord> findByDeviceIdOrderByUpdateTimeDesc(Long deviceId);

    Optional<FirmwareRecord> findTopByDeviceIdOrderByUpdateTimeDesc(Long deviceId);
}
