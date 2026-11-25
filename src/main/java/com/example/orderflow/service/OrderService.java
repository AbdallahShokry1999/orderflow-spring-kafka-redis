package com.example.orderflow.service;

import com.example.orderflow.config.CacheConfig;
import com.example.orderflow.domain.Order;
import com.example.orderflow.dto.CreateOrderRequest;
import com.example.orderflow.dto.OrderResponse;
import com.example.orderflow.event.OrderCreatedEvent;
import com.example.orderflow.exception.OrderNotFoundException;
import com.example.orderflow.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.amount(),
                request.currency().toUpperCase(Locale.ROOT));

        Order saved = orderRepository.save(order);

        // handed to OrderEventProducer, which sends it to Kafka only after this transaction commits
        eventPublisher.publishEvent(new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                saved.getId(),
                saved.getCustomerId(),
                saved.getAmount(),
                saved.getCurrency()));

        log.info("Order {} created for customer {}", saved.getId(), saved.getCustomerId());
        return OrderResponse.from(saved);
    }

    /**
     * Cache-Aside through Spring's cache abstraction: on a hit this method body is skipped
     * entirely, so the log line below only appears on a cache MISS.
     */
    @Cacheable(cacheNames = CacheConfig.ORDERS, key = "#id")
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String id) {
        log.info("Cache miss for order {} - loading it from the database", id);
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
