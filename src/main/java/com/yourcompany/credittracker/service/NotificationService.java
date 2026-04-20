package com.yourcompany.credittracker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@example.com}")
    private String from;

    public void send(String to, String subject, String body) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email not sent because SMTP is not configured. to={} subject={} body={}", to, subject, body);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (RuntimeException ex) {
            log.warn("Email delivery failed. to={} subject={} error={}", to, subject, ex.getMessage());
        }
    }
}
