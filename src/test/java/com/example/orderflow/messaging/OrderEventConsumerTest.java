package com.example.orderflow.messaging;

import com.example.orderflow.config.CacheConfig;
import com.example.orderflow.event.OrderCreatedEvent;
import com.example.orderflow.service.OrderProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderProcessingService processingService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private OrderEventConsumer consumer;

    private final OrderCreatedEvent event =
            new OrderCreatedEvent("event-1", "order-1", "customer-1", new BigDecimal("10.00"), "EGP");

    @Test
    void givenEvent_whenConsumed_thenProcessThenEvictCacheThenAck() {
        // Arrange
        when(cacheManager.getCache(CacheConfig.ORDERS)).thenReturn(cache);

        // Act
        consumer.onOrderCreated(event, acknowledgment);

        // Assert: the order of the three steps matters
        InOrder inOrder = inOrder(processingService, cache, acknowledgment);
        inOrder.verify(processingService).process(event);
        inOrder.verify(cache).evict("order-1");
        inOrder.verify(acknowledgment).acknowledge();
    }

    @Test
    void givenProcessingFails_whenConsumed_thenDoNotAck() {
        // Arrange
        doThrow(new IllegalStateException("boom")).when(processingService).process(event);

        // Act & Assert: the exception goes to the error handler (retry, then DLT)
        assertThatThrownBy(() -> consumer.onOrderCreated(event, acknowledgment))
                .isInstanceOf(IllegalStateException.class);

        verify(acknowledgment, never()).acknowledge();
    }
}
