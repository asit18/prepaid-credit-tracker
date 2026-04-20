package com.yourcompany.credittracker.service;

import com.yourcompany.credittracker.model.AuditEvent;
import com.yourcompany.credittracker.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "SECURITY_AUDIT")
@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public void record(String eventType, String actorEmail, String targetEmail, String details) {
        AuditEvent event = new AuditEvent();
        event.setEventType(eventType);
        event.setActorEmail(actorEmail == null ? "system" : actorEmail);
        event.setTargetEmail(targetEmail);
        event.setDetails(details);
        auditEventRepository.save(event);
        log.info("event={} actor={} target={} details={}", eventType, event.getActorEmail(), targetEmail, details);
    }

    public void mfaSetupStarted(Long userId, String email) {
        record("mfa_setup_started", email, email, "userId=" + userId);
        log.info("event=mfa_setup_started userId={} email={}", userId, email);
    }

    public void mfaSetupConfirmed(Long userId, String email) {
        record("mfa_setup_confirmed", email, email, "userId=" + userId);
        log.info("event=mfa_setup_confirmed userId={} email={}", userId, email);
    }

    public void mfaVerificationSuccess(Long userId, String email) {
        record("mfa_verification_success", email, email, "userId=" + userId);
        log.info("event=mfa_verification_success userId={} email={}", userId, email);
    }

    public void mfaVerificationFailure(Long userId, String email) {
        record("mfa_verification_failure", email, email, "userId=" + userId);
        log.warn("event=mfa_verification_failure userId={} email={}", userId, email);
    }

    public void recoveryCodeUsed(Long userId, String email) {
        record("mfa_recovery_code_used", email, email, "userId=" + userId);
        log.warn("event=mfa_recovery_code_used userId={} email={}", userId, email);
    }

    public void mfaDisabled(Long userId, String email) {
        record("mfa_disabled", email, email, "userId=" + userId);
        log.warn("event=mfa_disabled userId={} email={}", userId, email);
    }
}
