package com.microservices.email.service;

import com.microservices.email.entity.Email;
import com.microservices.email.event.OrderEvent;
import com.microservices.email.repository.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private EmailRepository emailRepository;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private EmailSenderService emailSenderService;

    public void processOrderEvent(OrderEvent orderEvent) {
        try {
            logger.info("Processing order event for order: {} with status: {}",
                       orderEvent.getOrderId(), orderEvent.getOrderStatus());

            Email.EmailType emailType = mapOrderStatusToEmailType(orderEvent.getOrderStatus());

            if (emailType == null) {
                logger.warn("No email type mapping found for order status: {}", orderEvent.getOrderStatus());
                return;
            }

            // Check if email already exists for this order and email type to avoid duplicates
            Optional<Email> existingEmail = emailRepository.findByOrderIdAndEmailType(
                orderEvent.getOrderId(), emailType);

            if (existingEmail.isPresent()) {
                logger.info("Email already exists for order: {} and type: {}",
                           orderEvent.getOrderId(), emailType);
                return;
            }

            // Create and send email
            Email email = createEmailFromOrderEvent(orderEvent, emailType);
            Email savedEmail = emailRepository.save(email);

            // Send email asynchronously
            sendEmailAsync(savedEmail, orderEvent);

            logger.info("Email record created with ID: {} for order: {}",
                       savedEmail.getId(), orderEvent.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing order event for order: {}", orderEvent.getOrderId(), e);
            throw new RuntimeException("Failed to process order event", e);
        }
    }

    @Async
    public CompletableFuture<Void> sendEmailAsync(Email email, OrderEvent orderEvent) {
        try {
            sendEmail(email, orderEvent);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            logger.error("Error in async email sending for email ID: {}", email.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public void sendEmail(Email email, OrderEvent orderEvent) {
        try {
            logger.info("Attempting to send email ID: {}", email.getId());

            // Update status to indicate sending attempt
            email.setEmailStatus(Email.EmailStatus.RETRYING);
            emailRepository.save(email);

            // Send the email
            boolean success = emailSenderService.sendEmail(
                email.getEmailAddress(),
                email.getSubject(),
                email.getContent()
            );

            if (success) {
                email.setEmailStatus(Email.EmailStatus.SENT);
                email.setSentAt(LocalDateTime.now());
                logger.info("Email sent successfully for email ID: {}", email.getId());
            } else {
                email.setEmailStatus(Email.EmailStatus.FAILED);
                logger.error("Failed to send email ID: {}", email.getId());
            }

            emailRepository.save(email);

        } catch (Exception e) {
            email.setEmailStatus(Email.EmailStatus.FAILED);
            emailRepository.save(email);
            logger.error("Error sending email ID: {}", email.getId(), e);
            throw e;
        }
    }

    public void retryFailedEmails() {
        logger.info("Starting retry process for failed emails");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(1); // Retry emails older than 1 hour
        List<Email> failedEmails = emailRepository.findFailedEmailsOlderThan(
            Email.EmailStatus.FAILED, cutoffTime);

        logger.info("Found {} failed emails to retry", failedEmails.size());

        failedEmails.forEach(email -> {
            try {
                // For retry, we need to recreate the order event or store necessary data
                // For simplicity, we'll just resend with existing content
                boolean success = emailSenderService.sendEmail(
                    email.getEmailAddress(),
                    email.getSubject(),
                    email.getContent()
                );

                if (success) {
                    email.setEmailStatus(Email.EmailStatus.SENT);
                    email.setSentAt(LocalDateTime.now());
                    logger.info("Successfully retried email ID: {}", email.getId());
                } else {
                    logger.warn("Retry failed for email ID: {}", email.getId());
                }

                emailRepository.save(email);

            } catch (Exception e) {
                logger.error("Error retrying email ID: {}", email.getId(), e);
            }
        });

        logger.info("Completed retry process for failed emails");
    }

    public List<Email> getEmailsByOrderId(Long orderId) {
        return emailRepository.findByOrderId(orderId);
    }

    public List<Email> getEmailsByCustomerId(String customerId) {
        return emailRepository.findByCustomerId(customerId);
    }

    public List<Email> getEmailsByStatus(Email.EmailStatus status) {
        return emailRepository.findByEmailStatus(status);
    }

    public long getEmailCountByStatus(Email.EmailStatus status) {
        return emailRepository.countByEmailStatus(status);
    }

    private Email createEmailFromOrderEvent(OrderEvent orderEvent, Email.EmailType emailType) {
        String subject = emailTemplateService.generateSubject(emailType, orderEvent);
        String content = emailTemplateService.generateContent(emailType, orderEvent);

        return new Email(
            orderEvent.getOrderId(),
            orderEvent.getCustomerId(),
            orderEvent.getCustomerEmail(),
            emailType,
            subject,
            content
        );
    }

    private Email.EmailType mapOrderStatusToEmailType(OrderEvent.OrderStatus orderStatus) {
        return switch (orderStatus) {
            case CONFIRMED, PAID -> Email.EmailType.ORDER_CONFIRMATION;
            case SHIPPED -> Email.EmailType.ORDER_SHIPPED;
            case DELIVERED -> Email.EmailType.ORDER_DELIVERED;
            case CANCELLED -> Email.EmailType.ORDER_CANCELLED;
            case REFUNDED -> Email.EmailType.ORDER_REFUNDED;
            case FAILED -> Email.EmailType.PAYMENT_FAILED;
            default -> null; // No email for other statuses like CREATED, PROCESSING, etc.
        };
    }
}
