package com.avledger.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String message = "数据完整性校验失败";
        String rootMsg = e.getMostSpecificCause().getMessage();
        if (rootMsg != null) {
            if (rootMsg.contains("purchase_date")) {
                message = "购入日期不能为空";
            } else if (rootMsg.contains("name")) {
                message = "设备名称不能为空";
            } else if (rootMsg.contains("model")) {
                message = "设备型号不能为空";
            } else if (rootMsg.contains("device_type")) {
                message = "设备类型不能为空";
            } else if (rootMsg.contains("status")) {
                message = "设备状态不能为空";
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }
}
