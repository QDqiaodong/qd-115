package com.avledger.repository;

import com.avledger.entity.CableConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CableConnectionRepository extends JpaRepository<CableConnection, Long> {

    List<CableConnection> findBySourceDeviceIdOrderByIdDesc(Long sourceDeviceId);

    List<CableConnection> findByTargetDeviceIdOrderByIdDesc(Long targetDeviceId);

    @Query("SELECT c FROM CableConnection c WHERE c.sourceDevice.id = :deviceId OR c.targetDevice.id = :deviceId ORDER BY c.id DESC")
    List<CableConnection> findByDeviceId(@Param("deviceId") Long deviceId);

    @Query("SELECT c FROM CableConnection c WHERE (:deviceId IS NULL OR c.sourceDevice.id = :deviceId OR c.targetDevice.id = :deviceId) ORDER BY c.id DESC")
    List<CableConnection> findAllByDeviceIdOptional(@Param("deviceId") Long deviceId);
}
