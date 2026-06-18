package com.avledger.entity;

import com.avledger.enums.MaintenanceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "maintenance_record", uniqueConstraints = {
        @UniqueConstraint(name = "uk_device_type_date", columnNames = {"device_id", "maintenance_type", "maintenance_date"})
})
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Device device;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime maintenanceTime;

    @Column(name = "maintenance_date", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maintenanceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 20)
    private MaintenanceType maintenanceType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 50)
    private String operator;

    @Column(length = 255)
    private String remark;

    @CreationTimestamp
    @Column(updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void syncMaintenanceDate() {
        if (this.maintenanceTime != null) {
            this.maintenanceDate = this.maintenanceTime.toLocalDate();
        }
    }
}
