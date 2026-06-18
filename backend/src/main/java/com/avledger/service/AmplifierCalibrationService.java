package com.avledger.service;

import com.avledger.entity.AmplifierCalibration;
import com.avledger.entity.Device;
import com.avledger.enums.DeviceType;
import com.avledger.repository.AmplifierCalibrationRepository;
import com.avledger.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AmplifierCalibrationService {

    private final AmplifierCalibrationRepository amplifierCalibrationRepository;
    private final DeviceRepository deviceRepository;

    public List<AmplifierCalibration> findAll(Long amplifierId) {
        return amplifierCalibrationRepository.findAllByAmplifierIdOptional(amplifierId);
    }

    public List<AmplifierCalibration> findByAmplifierId(Long amplifierId) {
        return amplifierCalibrationRepository.findByAmplifierIdOrderByCalibrationDateDesc(amplifierId);
    }

    public Optional<AmplifierCalibration> findById(Long id) {
        return amplifierCalibrationRepository.findById(id);
    }

    public Optional<AmplifierCalibration> findLatestByAmplifierId(Long amplifierId) {
        return amplifierCalibrationRepository.findTopByAmplifierIdOrderByCalibrationDateDesc(amplifierId);
    }

    public List<AmplifierCalibration> findByAmplifierIdAndChannelName(Long amplifierId, String channelName) {
        return amplifierCalibrationRepository.findByAmplifierIdAndChannelNameOrderByCalibrationDateDesc(amplifierId, channelName);
    }

    @Transactional
    public AmplifierCalibration save(AmplifierCalibration calibration, Long amplifierId) {
        Device amplifier = deviceRepository.findById(amplifierId)
                .orElseThrow(() -> new IllegalArgumentException("功放设备不存在，id: " + amplifierId));
        if (amplifier.getDeviceType() != DeviceType.AMPLIFIER) {
            throw new IllegalArgumentException("所选设备不是功放类型");
        }
        calibration.setAmplifier(amplifier);
        return amplifierCalibrationRepository.save(calibration);
    }

    @Transactional
    public Optional<AmplifierCalibration> update(Long id, AmplifierCalibration calibration, Long amplifierId) {
        return amplifierCalibrationRepository.findById(id).map(existing -> {
            existing.setChannelName(calibration.getChannelName());
            existing.setVolumeReference(calibration.getVolumeReference());
            existing.setDistance(calibration.getDistance());
            existing.setCrossoverPoint(calibration.getCrossoverPoint());
            existing.setCalibrationDate(calibration.getCalibrationDate());
            existing.setCalibrationMethod(calibration.getCalibrationMethod());
            existing.setRemark(calibration.getRemark());

            if (amplifierId != null) {
                Device amplifier = deviceRepository.findById(amplifierId)
                        .orElseThrow(() -> new IllegalArgumentException("功放设备不存在"));
                if (amplifier.getDeviceType() != DeviceType.AMPLIFIER) {
                    throw new IllegalArgumentException("所选设备不是功放类型");
                }
                existing.setAmplifier(amplifier);
            }

            return amplifierCalibrationRepository.save(existing);
        });
    }

    @Transactional
    public boolean delete(Long id) {
        if (amplifierCalibrationRepository.existsById(id)) {
            amplifierCalibrationRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
