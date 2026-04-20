package com.yourcompany.credittracker.repository;

import com.yourcompany.credittracker.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
