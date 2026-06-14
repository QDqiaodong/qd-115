package com.avledger.repository;

import com.avledger.entity.RepairRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairRecordRepository extends JpaRepository<RepairRecord, Long> {

    List<RepairRecord> findByDeviceIdOrderByRepairTimeDesc(Long deviceId);

    List<RepairRecord> findTop10ByOrderByRepairTimeDesc();
}
