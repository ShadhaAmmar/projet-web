package com.iatechnology.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action; // e.g., CREATE, UPDATE, DELETE

    @Column(nullable = false)
    private String entityName; // e.g., Chercheur, Publication

    @Column(nullable = false)
    private String username; // The user who performed the action

    @Column(nullable = false)
    private LocalDateTime timestamp;
}
