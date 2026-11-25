package com.example.orderflow.messaging;

import com.example.orderflow.config.CacheConfig;
import com.example.orderflow.event.OrderCreatedEvent;
import com.example.orderflow.service.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderProcessingService processingService;
    private final CacheManager cacheManager;

    public OrderEventConsumer(OrderProcessingService processingService, CacheManager cacheManager) {
        this.processingService = processingService;
        this.cacheManager = cacheManager;
    }

    @KafkaListener(topics = Topics.ORDERS_CREATED)
    public void onOrderCreated(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        // 1. business work + idempotency record, in ONE database transaction (committed on return)
        processingService.process(event);

        // 2. the order's status changed behind the cache's back, so drop the stale entry.
        //    Done after the commit on purpose, and also for duplicates: evicting is harmless.
        Cache cache = cacheManager.getCache(CacheConfig.ORDERS);
        if (cache != null) {
            cache.evict(event.orderId());
        }

        // 3. only now tell Kafka the record is done (manual ack)
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = Topics.ORDERS_CREATED_DLT, groupId = "orderflow-dlt-group")
    public void onDeadLetter(OrderCreatedEvent event, Acknowledgment acknowledgment) {
        log.error("Dead letter received (retries exhausted): eventId={}, orderId={}",
                event.eventId(), event.orderId());
        acknowledgment.acknowledge();
    }
}
