package com.learning.exception;

public class OrderAlreadyExistsException extends RuntimeException {
    public OrderAlreadyExistsException(String orderId) {
        super("Order " + orderId + " already exists");
    }
}
