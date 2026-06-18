package com.avledger.controller;

import com.avledger.entity.AmplifierCalibration;
import com.avledger.service.AmplifierCalibrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amplifier-calibrations")
@RequiredArgsConstructor
public class AmplifierCalibrationController {

    private final AmplifierCalibrationService amplifierCalibrationService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<AmplifierCalibration>> findAll(@RequestParam(required = false) Long amplifierId) {
        return ResponseEntity.ok(amplifierCalibrationService.findAll(amplifierId));
    }

    @GetMapping("/amplifier/{amplifierId}")
    public ResponseEntity<List<AmplifierCalibration>> findByAmplifierId(@PathVariable Long amplifierId) {
        return ResponseEntity.ok(amplifierCalibrationService.findByAmplifierId(amplifierId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AmplifierCalibration> findById(@PathVariable Long id) {
        return amplifierCalibrationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/amplifier/{amplifierId}/latest")
    public ResponseEntity<AmplifierCalibration> findLatestByAmplifierId(@PathVariable Long amplifierId) {
        return amplifierCalibrationService.findLatestByAmplifierId(amplifierId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AmplifierCalibration> create(@RequestBody Map<String, Object> payload) {
        Long amplifierId = Long.valueOf(payload.get("amplifierId").toString());
        payload.remove("amplifierId");
        AmplifierCalibration calibration = objectMapper.convertValue(payload, AmplifierCalibration.class);
        return ResponseEntity.ok(amplifierCalibrationService.save(calibration, amplifierId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AmplifierCalibration> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Long amplifierId = payload.get("amplifierId") != null ? Long.valueOf(payload.get("amplifierId").toString()) : null;
        payload.remove("amplifierId");
        AmplifierCalibration calibration = objectMapper.convertValue(payload, AmplifierCalibration.class);
        return amplifierCalibrationService.update(id, calibration, amplifierId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return amplifierCalibrationService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
