package com.avledger.entity;

import com.avledger.enums.DeviceStatus;
import com.avledger.enums.DeviceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "device")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Column(length = 100)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String hardwareSpecs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.NORMAL;

    @Column(length = 255)
    private String statusChangeSource;

    private LocalDate lampInstallDate;

    private Integer lampReplaceHours;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
