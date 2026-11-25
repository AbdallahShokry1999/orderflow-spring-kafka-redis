package com.example.orderflow.messaging;

import com.example.orderflow.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the Kafka message only AFTER the order's database transaction has committed
 * (TransactionalEventListener defaults to the AFTER_COMMIT phase). That avoids publishing an
 * event for an order that was rolled back.
 *
 * Known gap: if the app crashes between the commit and the send, the event is lost
 * (the "dual write" problem). The production fix is the Transactional Outbox pattern.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        // the key is the orderId, so all events of one order go to the same partition, in order
        kafkaTemplate.send(Topics.ORDERS_CREATED, event.orderId(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} for order {}", event.eventId(), event.orderId(), ex);
                    } else {
                        log.info("Published event {} for order {}", event.eventId(), event.orderId());
                    }
                });
    }
}
