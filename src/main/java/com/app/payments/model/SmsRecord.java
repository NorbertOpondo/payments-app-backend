package com.app.payments.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String transactionId;

    /** 0 = pending first attempt, 200 = sent, 400 = failed, 500 = terminal failure */
    private int status;

    private int retryCount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
