package com.example.orderflow.dto;

import com.example.orderflow.domain.Order;
import com.example.orderflow.domain.OrderStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Serializable because this is the object stored in Redis (Spring's default Redis cache
 * serializer uses JDK serialization). We cache this DTO, never the JPA entity.
 */
public record OrderResponse(
        String id,
        String customerId,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        Instant createdAt) implements Serializable {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getCreatedAt());
    }
}
