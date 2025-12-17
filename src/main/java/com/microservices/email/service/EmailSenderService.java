package com.microservices.email.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailSenderService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSenderService.class);

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.from:noreply@emailservice.com}")
    private String fromEmail;

    @Value("${email.service.enabled:true}")
    private boolean emailServiceEnabled;

    @Value("${email.service.mock:false}")
    private boolean mockEmailSending;

    public EmailSenderService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public boolean sendEmail(String to, String subject, String content) {
        if (!emailServiceEnabled) {
            logger.info("Email service is disabled. Skipping email send to: {}", to);
            return true;
        }

        if (mockEmailSending) {
            return mockSendEmail(to, subject, content);
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            javaMailSender.send(message);

            logger.info("Email sent successfully to: {} with subject: {}", to, subject);
            return true;

        } catch (Exception e) {
            logger.error("Failed to send email to: {} with subject: {}", to, subject, e);
            return false;
        }
    }

    private boolean mockSendEmail(String to, String subject, String content) {
        logger.info("MOCK EMAIL SEND:");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("Content: {}", content);
        logger.info("--- END MOCK EMAIL ---");

        // Simulate some processing time
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return true;
    }

    public boolean sendBulkEmails(java.util.List<SimpleMailMessage> messages) {
        if (!emailServiceEnabled) {
            logger.info("Email service is disabled. Skipping bulk email send");
            return true;
        }

        try {
            javaMailSender.send(messages.toArray(new SimpleMailMessage[0]));
            logger.info("Bulk emails sent successfully. Count: {}", messages.size());
            return true;

        } catch (Exception e) {
            logger.error("Failed to send bulk emails", e);
            return false;
        }
    }
}
