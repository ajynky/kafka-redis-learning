package com.learning.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("Order " + orderId + " not found");
    }
}
