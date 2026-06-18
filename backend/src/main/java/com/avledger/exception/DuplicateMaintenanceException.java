package com.avledger.exception;

import com.avledger.entity.MaintenanceRecord;
import lombok.Getter;

import java.util.List;

@Getter
public class DuplicateMaintenanceException extends RuntimeException {

    private final List<MaintenanceRecord> existingRecords;

    public DuplicateMaintenanceException(String message, List<MaintenanceRecord> existingRecords) {
        super(message);
        this.existingRecords = existingRecords;
    }
}
