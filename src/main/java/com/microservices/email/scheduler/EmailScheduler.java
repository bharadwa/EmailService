package com.microservices.email.scheduler;

import com.microservices.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmailScheduler.class);

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void retryFailedEmails() {
        try {
            logger.info("Starting scheduled retry of failed emails");
            emailService.retryFailedEmails();
        } catch (Exception e) {
            logger.error("Error during scheduled retry of failed emails", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *") // Every day at 2 AM
    public void dailyEmailCleanup() {
        try {
            logger.info("Starting daily email cleanup process");
            // Add cleanup logic here (e.g., archive old emails, clean up logs)
            logger.info("Daily email cleanup completed");
        } catch (Exception e) {
            logger.error("Error during daily email cleanup", e);
        }
    }
}
