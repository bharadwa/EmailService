package com.microservices.email.controller;

import com.microservices.email.entity.Email;
import com.microservices.email.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Email>> getEmailsByOrderId(@PathVariable Long orderId) {
        List<Email> emails = emailService.getEmailsByOrderId(orderId);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<Email>> getEmailsByCustomerId(@PathVariable String customerId) {
        List<Email> emails = emailService.getEmailsByCustomerId(customerId);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Email>> getEmailsByStatus(@PathVariable Email.EmailStatus status) {
        List<Email> emails = emailService.getEmailsByStatus(status);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEmailStats() {
        Map<String, Object> stats = Map.of(
            "pending", emailService.getEmailCountByStatus(Email.EmailStatus.PENDING),
            "sent", emailService.getEmailCountByStatus(Email.EmailStatus.SENT),
            "failed", emailService.getEmailCountByStatus(Email.EmailStatus.FAILED),
            "retrying", emailService.getEmailCountByStatus(Email.EmailStatus.RETRYING)
        );
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/retry-failed")
    public ResponseEntity<String> retryFailedEmails() {
        try {
            emailService.retryFailedEmails();
            return ResponseEntity.ok("Retry process initiated successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("Error initiating retry process: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "email-service",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}
