package com.example.orderflow.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row per Kafka event that was already handled. The primary key (eventId) is what makes
 * the consumer idempotent: a second insert of the same eventId violates the key and fails.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @Id
    @Column(length = 36)
    private String eventId;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
        // required by JPA
    }

    public ProcessedEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() { return eventId; }
    public Instant getProcessedAt() { return processedAt; }
}
