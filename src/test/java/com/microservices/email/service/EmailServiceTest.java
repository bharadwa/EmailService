package com.microservices.email.service;

import com.microservices.email.entity.Email;
import com.microservices.email.event.OrderEvent;
import com.microservices.email.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private EmailRepository emailRepository;

    @Mock
    private EmailTemplateService emailTemplateService;

    @Mock
    private EmailSenderService emailSenderService;

    @InjectMocks
    private EmailService emailService;

    private OrderEvent testOrderEvent;

    @BeforeEach
    void setUp() {
        testOrderEvent = new OrderEvent();
        testOrderEvent.setOrderId(12345L);
        testOrderEvent.setCustomerId("CUST001");
        testOrderEvent.setCustomerEmail("test@example.com");
        testOrderEvent.setCustomerName("John Doe");
        testOrderEvent.setOrderStatus(OrderEvent.OrderStatus.CONFIRMED);
        testOrderEvent.setTotalAmount(new BigDecimal("99.99"));
        testOrderEvent.setCurrency("USD");
        testOrderEvent.setOrderDate(LocalDateTime.now());
        testOrderEvent.setEventType("ORDER_CONFIRMED");
        testOrderEvent.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testProcessOrderEvent_NewEmail_Success() {
        // Arrange
        when(emailRepository.findByOrderIdAndEmailType(any(), any())).thenReturn(Optional.empty());
        when(emailTemplateService.generateSubject(any(), any())).thenReturn("Order Confirmation");
        when(emailTemplateService.generateContent(any(), any())).thenReturn("Thank you for your order");
        when(emailRepository.save(any())).thenReturn(createMockEmail());

        // Act
        emailService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailRepository).findByOrderIdAndEmailType(12345L, Email.EmailType.ORDER_CONFIRMATION);
        verify(emailRepository).save(any(Email.class));
        verify(emailTemplateService).generateSubject(Email.EmailType.ORDER_CONFIRMATION, testOrderEvent);
        verify(emailTemplateService).generateContent(Email.EmailType.ORDER_CONFIRMATION, testOrderEvent);
    }

    @Test
    void testProcessOrderEvent_DuplicateEmail_SkipsCreation() {
        // Arrange
        when(emailRepository.findByOrderIdAndEmailType(any(), any())).thenReturn(Optional.of(createMockEmail()));

        // Act
        emailService.processOrderEvent(testOrderEvent);

        // Assert
        verify(emailRepository).findByOrderIdAndEmailType(12345L, Email.EmailType.ORDER_CONFIRMATION);
        verify(emailRepository, never()).save(any(Email.class));
    }

    @Test
    void testSendEmail_Success() {
        // Arrange
        Email email = createMockEmail();
        when(emailSenderService.sendEmail(any(), any(), any())).thenReturn(true);
        when(emailRepository.save(any())).thenReturn(email);

        // Act
        emailService.sendEmail(email, testOrderEvent);

        // Assert
        verify(emailSenderService).sendEmail(email.getEmailAddress(), email.getSubject(), email.getContent());
        verify(emailRepository, times(2)).save(email); // Once for RETRYING, once for SENT
        assertEquals(Email.EmailStatus.SENT, email.getEmailStatus());
        assertNotNull(email.getSentAt());
    }

    @Test
    void testSendEmail_Failure() {
        // Arrange
        Email email = createMockEmail();
        when(emailSenderService.sendEmail(any(), any(), any())).thenReturn(false);
        when(emailRepository.save(any())).thenReturn(email);

        // Act
        emailService.sendEmail(email, testOrderEvent);

        // Assert
        verify(emailSenderService).sendEmail(email.getEmailAddress(), email.getSubject(), email.getContent());
        verify(emailRepository, times(2)).save(email); // Once for RETRYING, once for FAILED
        assertEquals(Email.EmailStatus.FAILED, email.getEmailStatus());
    }

    private Email createMockEmail() {
        Email email = new Email();
        email.setId(1L);
        email.setOrderId(12345L);
        email.setCustomerId("CUST001");
        email.setEmailAddress("test@example.com");
        email.setEmailType(Email.EmailType.ORDER_CONFIRMATION);
        email.setSubject("Order Confirmation");
        email.setContent("Thank you for your order");
        email.setEmailStatus(Email.EmailStatus.PENDING);
        email.setCreatedAt(LocalDateTime.now());
        return email;
    }
}
