package com.example.orderflow.service;

import com.example.orderflow.domain.Order;
import com.example.orderflow.domain.OrderStatus;
import com.example.orderflow.domain.ProcessedEvent;
import com.example.orderflow.event.OrderCreatedEvent;
import com.example.orderflow.exception.OrderNotFoundException;
import com.example.orderflow.repository.OrderRepository;
import com.example.orderflow.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderProcessingServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private OrderProcessingService processingService;

    private OrderCreatedEvent eventFor(String customerId) {
        return new OrderCreatedEvent("event-1", "order-1", customerId, new BigDecimal("10.00"), "EGP");
    }

    @Test
    void givenNewEvent_whenProcess_thenMarkOrderProcessedAndRecordEvent() {
        // Arrange
        Order order = new Order("order-1", "customer-1", new BigDecimal("10.00"), "EGP");
        when(processedEventRepository.existsById("event-1")).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        // Act
        processingService.process(eventFor("customer-1"));

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSED);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void givenAlreadyProcessedEvent_whenProcess_thenSkipEverything() {
        // Arrange
        when(processedEventRepository.existsById("event-1")).thenReturn(true);

        // Act
        processingService.process(eventFor("customer-1"));

        // Assert: the duplicate is ignored, nothing is read or written
        verify(orderRepository, never()).findById(any());
        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void givenPoisonCustomer_whenProcess_thenThrowSoRetryAndDltKickIn() {
        // Act & Assert
        assertThatThrownBy(() -> processingService.process(eventFor("fail-demo")))
                .isInstanceOf(IllegalStateException.class);

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void givenEventForMissingOrder_whenProcess_thenThrowOrderNotFound() {
        // Arrange
        when(processedEventRepository.existsById("event-1")).thenReturn(false);
        when(orderRepository.findById("order-1")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> processingService.process(eventFor("customer-1")))
                .isInstanceOf(OrderNotFoundException.class);
    }
}
