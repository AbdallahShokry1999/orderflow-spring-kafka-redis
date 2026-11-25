package com.example.orderflow.controller;

import com.example.orderflow.domain.OrderStatus;
import com.example.orderflow.dto.CreateOrderRequest;
import com.example.orderflow.dto.OrderResponse;
import com.example.orderflow.exception.OrderNotFoundException;
import com.example.orderflow.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderResponse sampleResponse() {
        return new OrderResponse("order-1", "customer-1", new BigDecimal("150.00"), "EGP",
                OrderStatus.CREATED, Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void givenValidRequest_whenCreateOrder_thenReturn201WithLocation() throws Exception {
        // Arrange
        given(orderService.createOrder(any(CreateOrderRequest.class))).willReturn(sampleResponse());
        CreateOrderRequest request = new CreateOrderRequest("customer-1", new BigDecimal("150.00"), "EGP");

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/order-1"))
                .andExpect(jsonPath("$.id").value("order-1"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void givenInvalidRequest_whenCreateOrder_thenReturn400() throws Exception {
        // Arrange: blank customer, negative amount, currency that is not 3 letters
        CreateOrderRequest invalid = new CreateOrderRequest("", new BigDecimal("-5"), "EG");

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Validation failed"));
    }

    @Test
    void givenExistingOrder_whenGetOrder_thenReturn200() throws Exception {
        // Arrange
        given(orderService.getOrder("order-1")).willReturn(sampleResponse());

        // Act & Assert
        mockMvc.perform(get("/api/orders/order-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-1"));
    }

    @Test
    void givenUnknownOrder_whenGetOrder_thenReturn404() throws Exception {
        // Arrange
        given(orderService.getOrder("nope")).willThrow(new OrderNotFoundException("nope"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order not found: nope"));
    }
}
