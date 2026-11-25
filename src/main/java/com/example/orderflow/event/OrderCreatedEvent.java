package com.example.orderflow.event;

import java.math.BigDecimal;

/**
 * Published to Kafka after an order is committed. Deliberately has no java.time fields so the
 * Jackson 2 based Spring Kafka JsonSerializer can handle it without extra modules.
 */
public record OrderCreatedEvent(
        String eventId,
        String orderId,
        String customerId,
        BigDecimal amount,
        String currency) {
}
