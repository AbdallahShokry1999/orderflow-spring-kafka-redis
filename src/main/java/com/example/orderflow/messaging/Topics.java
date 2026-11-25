package com.example.orderflow.messaging;

public final class Topics {

    public static final String ORDERS_CREATED = "orders.created";
    public static final String ORDERS_CREATED_DLT = ORDERS_CREATED + ".DLT";

    private Topics() {
    }
}
