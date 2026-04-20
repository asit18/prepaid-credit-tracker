package com.yourcompany.credittracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_events_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_events_event_type", columnList = "event_type")
})
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(name = "target_email")
    private String targetEmail;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
