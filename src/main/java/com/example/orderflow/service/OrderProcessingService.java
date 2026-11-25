package com.example.orderflow.service;

import com.example.orderflow.domain.Order;
import com.example.orderflow.domain.ProcessedEvent;
import com.example.orderflow.event.OrderCreatedEvent;
import com.example.orderflow.exception.OrderNotFoundException;
import com.example.orderflow.repository.OrderRepository;
import com.example.orderflow.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessingService.class);

    /** Demo hook: customers whose id starts with this prefix simulate a poison message. */
    static final String POISON_PREFIX = "fail";

    private final OrderRepository orderRepository;
    private final ProcessedEventRepository processedEventRepository;

    public OrderProcessingService(OrderRepository orderRepository,
                                  ProcessedEventRepository processedEventRepository) {
        this.orderRepository = orderRepository;
        this.processedEventRepository = processedEventRepository;
    }

    /**
     * Idempotent: handling the same event twice has the same effect as handling it once.
     * The processed-event row and the order update commit (or roll back) together.
     * If two consumers race on the same event, the primary key rejects the second insert,
     * that transaction rolls back, and the retry then sees the row and skips.
     */
    @Transactional
    public void process(OrderCreatedEvent event) {
        if (event.customerId().startsWith(POISON_PREFIX)) {
            throw new IllegalStateException("Simulated processing failure for event " + event.eventId());
        }

        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Event {} already processed - skipping", event.eventId());
            return;
        }

        processedEventRepository.save(new ProcessedEvent(event.eventId()));

        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(event.orderId()));
        order.markProcessed();

        log.info("Order {} processed (event {})", event.orderId(), event.eventId());
    }
}
