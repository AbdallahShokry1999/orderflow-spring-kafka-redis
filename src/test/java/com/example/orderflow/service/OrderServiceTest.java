package com.example.orderflow.service;

import com.example.orderflow.domain.Order;
import com.example.orderflow.domain.OrderStatus;
import com.example.orderflow.dto.CreateOrderRequest;
import com.example.orderflow.dto.OrderResponse;
import com.example.orderflow.event.OrderCreatedEvent;
import com.example.orderflow.exception.OrderNotFoundException;
import com.example.orderflow.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void givenValidRequest_whenCreateOrder_thenSaveOrderAndPublishEvent() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateOrderRequest request = new CreateOrderRequest("customer-1", new BigDecimal("150.00"), "egp");

        // Act
        OrderResponse response = orderService.createOrder(request);

        // Assert
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.currency()).isEqualTo("EGP");

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(response.id());
        assertThat(captor.getValue().customerId()).isEqualTo("customer-1");
    }

    @Test
    void givenExistingOrder_whenGetOrder_thenReturnResponse() {
        // Arrange
        Order order = new Order("order-1", "customer-1", new BigDecimal("10.00"), "EGP");
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        // Act
        OrderResponse response = orderService.getOrder("order-1");

        // Assert
        assertThat(response.id()).isEqualTo("order-1");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    void givenUnknownId_whenGetOrder_thenThrowOrderNotFound() {
        // Arrange
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrder("missing"))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("missing");
    }
}
