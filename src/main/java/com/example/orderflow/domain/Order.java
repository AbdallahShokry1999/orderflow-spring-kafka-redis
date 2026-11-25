package com.example.orderflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Order() {
        // required by JPA
    }

    public Order(String id, String customerId, BigDecimal amount, String currency) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.status = OrderStatus.CREATED;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void markProcessed() {
        this.status = OrderStatus.PROCESSED;
    }
}
