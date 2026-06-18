package com.avledger.repository;

import com.avledger.entity.AmplifierCalibration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmplifierCalibrationRepository extends JpaRepository<AmplifierCalibration, Long> {

    List<AmplifierCalibration> findByAmplifierIdOrderByCalibrationDateDesc(Long amplifierId);

    @Query("SELECT a FROM AmplifierCalibration a WHERE (:amplifierId IS NULL OR a.amplifier.id = :amplifierId) ORDER BY a.calibrationDate DESC")
    List<AmplifierCalibration> findAllByAmplifierIdOptional(@Param("amplifierId") Long amplifierId);

    Optional<AmplifierCalibration> findTopByAmplifierIdOrderByCalibrationDateDesc(Long amplifierId);

    List<AmplifierCalibration> findByAmplifierIdAndChannelNameOrderByCalibrationDateDesc(Long amplifierId, String channelName);
}
