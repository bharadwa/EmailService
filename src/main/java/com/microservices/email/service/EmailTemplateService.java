package com.microservices.email.service;

import com.microservices.email.entity.Email;
import com.microservices.email.event.OrderEvent;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");

    public String generateSubject(Email.EmailType emailType, OrderEvent orderEvent) {
        return switch (emailType) {
            case ORDER_CONFIRMATION -> "Order Confirmation - Order #" + orderEvent.getOrderId();
            case ORDER_SHIPPED -> "Your Order Has Been Shipped - Order #" + orderEvent.getOrderId();
            case ORDER_DELIVERED -> "Your Order Has Been Delivered - Order #" + orderEvent.getOrderId();
            case ORDER_CANCELLED -> "Order Cancelled - Order #" + orderEvent.getOrderId();
            case ORDER_REFUNDED -> "Refund Processed - Order #" + orderEvent.getOrderId();
            case PAYMENT_FAILED -> "Payment Failed - Order #" + orderEvent.getOrderId();
            case PROMOTIONAL -> "Special Offer Just For You!";
            case SYSTEM_NOTIFICATION -> "Important Account Notification";
        };
    }

    public String generateContent(Email.EmailType emailType, OrderEvent orderEvent) {
        return switch (emailType) {
            case ORDER_CONFIRMATION -> generateOrderConfirmationContent(orderEvent);
            case ORDER_SHIPPED -> generateOrderShippedContent(orderEvent);
            case ORDER_DELIVERED -> generateOrderDeliveredContent(orderEvent);
            case ORDER_CANCELLED -> generateOrderCancelledContent(orderEvent);
            case ORDER_REFUNDED -> generateOrderRefundedContent(orderEvent);
            case PAYMENT_FAILED -> generatePaymentFailedContent(orderEvent);
            case PROMOTIONAL -> generatePromotionalContent(orderEvent);
            case SYSTEM_NOTIFICATION -> generateSystemNotificationContent(orderEvent);
        };
    }

    private String generateOrderConfirmationContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("Thank you for your order! We're excited to confirm that we've received your order and it's being processed.\n\n");
        content.append("Order Details:\n");
        content.append("Order Number: ").append(orderEvent.getOrderId()).append("\n");
        content.append("Order Date: ").append(orderEvent.getOrderDate() != null ? orderEvent.getOrderDate().format(DATE_FORMATTER) : "N/A").append("\n");
        content.append("Total Amount: ").append(formatCurrency(orderEvent.getTotalAmount(), orderEvent.getCurrency())).append("\n");
        content.append("Payment Method: ").append(orderEvent.getPaymentMethod() != null ? orderEvent.getPaymentMethod() : "N/A").append("\n\n");

        if (orderEvent.getItems() != null && !orderEvent.getItems().isEmpty()) {
            content.append("Items Ordered:\n");
            orderEvent.getItems().forEach(item -> {
                content.append("- ").append(item.getProductName())
                       .append(" (Qty: ").append(item.getQuantity())
                       .append(", Price: ").append(formatCurrency(item.getUnitPrice(), orderEvent.getCurrency()))
                       .append(")\n");
            });
            content.append("\n");
        }

        if (orderEvent.getShippingAddress() != null) {
            content.append("Shipping Address:\n");
            content.append(formatAddress(orderEvent.getShippingAddress())).append("\n\n");
        }

        content.append("We'll send you another email with tracking information once your order ships.\n\n");
        content.append("Thank you for choosing us!\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String generateOrderShippedContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("Great news! Your order has been shipped and is on its way to you.\n\n");
        content.append("Order Details:\n");
        content.append("Order Number: ").append(orderEvent.getOrderId()).append("\n");
        content.append("Tracking Number: ").append(orderEvent.getTrackingNumber() != null ? orderEvent.getTrackingNumber() : "Will be provided shortly").append("\n");
        content.append("Estimated Delivery: 3-5 business days\n\n");

        if (orderEvent.getShippingAddress() != null) {
            content.append("Shipping to:\n");
            content.append(formatAddress(orderEvent.getShippingAddress())).append("\n\n");
        }

        content.append("You can track your package using the tracking number provided above.\n\n");
        content.append("Thank you for your business!\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String generateOrderDeliveredContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("Your order has been successfully delivered!\n\n");
        content.append("Order Details:\n");
        content.append("Order Number: ").append(orderEvent.getOrderId()).append("\n");
        content.append("Delivered on: ").append(orderEvent.getTimestamp().format(DATE_FORMATTER)).append("\n\n");

        content.append("We hope you're satisfied with your purchase. If you have any questions or concerns, ");
        content.append("please don't hesitate to contact our customer service team.\n\n");
        content.append("Thank you for choosing us and we look forward to serving you again!\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String generateOrderCancelledContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("We're writing to inform you that your order has been cancelled.\n\n");
        content.append("Order Details:\n");
        content.append("Order Number: ").append(orderEvent.getOrderId()).append("\n");
        content.append("Cancellation Date: ").append(orderEvent.getTimestamp().format(DATE_FORMATTER)).append("\n");
        content.append("Refund Amount: ").append(formatCurrency(orderEvent.getTotalAmount(), orderEvent.getCurrency())).append("\n\n");

        content.append("If you paid for this order, a full refund will be processed within 3-5 business days ");
        content.append("to your original payment method.\n\n");
        content.append("If you have any questions about this cancellation, please contact our customer service team.\n\n");
        content.append("Thank you for your understanding.\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String generateOrderRefundedContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("Your refund has been processed successfully.\n\n");
        content.append("Refund Details:\n");
        content.append("Order Number: ").append(orderEvent.getOrderId()).append("\n");
        content.append("Refund Amount: ").append(formatCurrency(orderEvent.getTotalAmount(), orderEvent.getCurrency())).append("\n");
        content.append("Processed Date: ").append(orderEvent.getTimestamp().format(DATE_FORMATTER)).append("\n\n");

        content.append("The refund will appear in your account within 3-5 business days, ");
        content.append("depending on your bank or payment provider.\n\n");
        content.append("If you have any questions about this refund, please contact our customer service team.\n\n");
        content.append("Thank you for your understanding.\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String generatePaymentFailedContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("We encountered an issue processing your payment for the following order:\n\n");
        content.append("Order Details:\n");
        content.append("Order Number: ").append(orderEvent.getOrderId()).append("\n");
        content.append("Amount: ").append(formatCurrency(orderEvent.getTotalAmount(), orderEvent.getCurrency())).append("\n");
        content.append("Payment Method: ").append(orderEvent.getPaymentMethod() != null ? orderEvent.getPaymentMethod() : "N/A").append("\n\n");

        content.append("Please update your payment information and try again. Your order will be held for 24 hours.\n\n");
        content.append("If you need assistance, please contact our customer service team.\n\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String generatePromotionalContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("We have an exciting offer just for you!\n\n");
        content.append("Don't miss out on our latest deals and promotions. ");
        content.append("Visit our website to discover amazing discounts on your favorite products.\n\n");
        content.append("Thank you for being a valued customer!\n");
        content.append("Marketing Team");

        return content.toString();
    }

    private String generateSystemNotificationContent(OrderEvent orderEvent) {
        StringBuilder content = new StringBuilder();
        content.append("Dear ").append(orderEvent.getCustomerName() != null ? orderEvent.getCustomerName() : "Valued Customer").append(",\n\n");
        content.append("This is an important notification regarding your account.\n\n");
        content.append("Please log in to your account to view the details.\n\n");
        content.append("If you have any questions, please contact our customer service team.\n\n");
        content.append("Customer Service Team");

        return content.toString();
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        if (amount == null) return "N/A";
        String currencySymbol = currency != null ? currency + " " : "$";
        return currencySymbol + amount.toString();
    }

    private String formatAddress(OrderEvent.Address address) {
        StringBuilder formattedAddress = new StringBuilder();
        if (address.getStreet() != null) formattedAddress.append(address.getStreet()).append("\n");
        if (address.getCity() != null) formattedAddress.append(address.getCity());
        if (address.getState() != null) formattedAddress.append(", ").append(address.getState());
        if (address.getZipCode() != null) formattedAddress.append(" ").append(address.getZipCode());
        if (address.getCountry() != null) formattedAddress.append("\n").append(address.getCountry());
        return formattedAddress.toString();
    }
}
