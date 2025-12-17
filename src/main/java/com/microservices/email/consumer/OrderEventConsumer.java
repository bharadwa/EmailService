package com.microservices.email.consumer;

import com.microservices.email.event.OrderEvent;
import com.microservices.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventConsumer.class);

    @Autowired
    private EmailService emailService;

    @KafkaListener(
        topics = "${kafka.topic.order-events:order-events}",
        groupId = "${spring.kafka.consumer.group-id:email-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderEvent(
            @Payload OrderEvent orderEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received order event from topic: {}, partition: {}, offset: {}, orderId: {}, status: {}",
                       topic, partition, offset, orderEvent.getOrderId(), orderEvent.getOrderStatus());

            // Validate the order event
            if (!isValidOrderEvent(orderEvent)) {
                logger.warn("Invalid order event received: {}", orderEvent.getOrderId());
                acknowledgment.acknowledge();
                return;
            }

            // Process the order event
            emailService.processOrderEvent(orderEvent);

            // Acknowledge the message after successful processing
            acknowledgment.acknowledge();

            logger.info("Successfully processed order event for orderId: {}", orderEvent.getOrderId());

        } catch (Exception e) {
            logger.error("Error processing order event from topic: {}, orderId: {}",
                        topic, orderEvent != null ? orderEvent.getOrderId() : "unknown", e);

            // In a production environment, you might want to:
            // 1. Send to a dead letter queue
            // 2. Implement retry mechanism
            // 3. Alert monitoring systems

            // For now, we'll acknowledge to prevent infinite reprocessing
            // In real production, you'd implement proper error handling
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(
        topics = "${kafka.topic.customer-events:customer-events}",
        groupId = "${spring.kafka.consumer.group-id:email-service-group}",
        containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void handleCustomerEvent(
            @Payload String customerEventJson,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received customer event from topic: {}", topic);

            // Process customer events (e.g., welcome emails, promotional emails)
            // Implementation would depend on the customer event structure

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Error processing customer event from topic: {}", topic, e);
            acknowledgment.acknowledge();
        }
    }

    private boolean isValidOrderEvent(OrderEvent orderEvent) {
        if (orderEvent == null) {
            logger.warn("Order event is null");
            return false;
        }

        if (orderEvent.getOrderId() == null) {
            logger.warn("Order event has null orderId");
            return false;
        }

        if (orderEvent.getCustomerId() == null || orderEvent.getCustomerId().trim().isEmpty()) {
            logger.warn("Order event has null or empty customerId for orderId: {}", orderEvent.getOrderId());
            return false;
        }

        if (orderEvent.getCustomerEmail() == null || orderEvent.getCustomerEmail().trim().isEmpty()) {
            logger.warn("Order event has null or empty customerEmail for orderId: {}", orderEvent.getOrderId());
            return false;
        }

        if (orderEvent.getOrderStatus() == null) {
            logger.warn("Order event has null orderStatus for orderId: {}", orderEvent.getOrderId());
            return false;
        }

        // Validate email format
        if (!isValidEmail(orderEvent.getCustomerEmail())) {
            logger.warn("Order event has invalid email format for orderId: {}, email: {}",
                       orderEvent.getOrderId(), orderEvent.getCustomerEmail());
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return email != null &&
               email.matches("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    }
}
