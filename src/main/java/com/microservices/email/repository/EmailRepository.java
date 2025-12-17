package com.microservices.email.repository;

import com.microservices.email.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    List<Email> findByOrderId(Long orderId);

    List<Email> findByCustomerId(String customerId);

    List<Email> findByEmailStatus(Email.EmailStatus emailStatus);

    List<Email> findByEmailType(Email.EmailType emailType);

    Optional<Email> findByOrderIdAndEmailType(Long orderId, Email.EmailType emailType);

    @Query("SELECT e FROM Email e WHERE e.emailStatus = :status AND e.createdAt < :cutoffTime")
    List<Email> findFailedEmailsOlderThan(@Param("status") Email.EmailStatus status,
                                         @Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT e FROM Email e WHERE e.emailStatus = :status AND e.createdAt BETWEEN :startTime AND :endTime")
    List<Email> findEmailsByStatusAndDateRange(@Param("status") Email.EmailStatus status,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(e) FROM Email e WHERE e.emailStatus = :status")
    long countByEmailStatus(@Param("status") Email.EmailStatus status);
}
