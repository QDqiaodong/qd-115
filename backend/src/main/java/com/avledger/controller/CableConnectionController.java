package com.avledger.controller;

import com.avledger.entity.CableConnection;
import com.avledger.service.CableConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cable-connections")
@RequiredArgsConstructor
public class CableConnectionController {

    private final CableConnectionService cableConnectionService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<List<CableConnection>> findAll(@RequestParam(required = false) Long deviceId) {
        return ResponseEntity.ok(cableConnectionService.findAll(deviceId));
    }

    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<CableConnection>> findByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(cableConnectionService.findByDeviceId(deviceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CableConnection> findById(@PathVariable Long id) {
        return cableConnectionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CableConnection> create(@RequestBody Map<String, Object> payload) {
        Long sourceDeviceId = Long.valueOf(payload.get("sourceDeviceId").toString());
        Long targetDeviceId = Long.valueOf(payload.get("targetDeviceId").toString());
        payload.remove("sourceDeviceId");
        payload.remove("targetDeviceId");
        CableConnection cableConnection = objectMapper.convertValue(payload, CableConnection.class);
        return ResponseEntity.ok(cableConnectionService.save(cableConnection, sourceDeviceId, targetDeviceId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CableConnection> update(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Long sourceDeviceId = payload.get("sourceDeviceId") != null ? Long.valueOf(payload.get("sourceDeviceId").toString()) : null;
        Long targetDeviceId = payload.get("targetDeviceId") != null ? Long.valueOf(payload.get("targetDeviceId").toString()) : null;
        payload.remove("sourceDeviceId");
        payload.remove("targetDeviceId");
        CableConnection cableConnection = objectMapper.convertValue(payload, CableConnection.class);
        return cableConnectionService.update(id, cableConnection, sourceDeviceId, targetDeviceId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return cableConnectionService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
