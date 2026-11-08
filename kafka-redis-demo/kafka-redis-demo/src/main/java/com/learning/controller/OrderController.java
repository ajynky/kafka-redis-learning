package com.learning.controller;

import com.learning.exception.OrderNotFoundException;
import com.learning.model.Order;
import com.learning.model.OrderStatusResponse;
import com.learning.repository.OrderRepository;
import com.learning.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> sendOrder(@RequestParam String orderId, @RequestParam String orderData) {
        orderService.createOrder(orderId, orderData);
        return ResponseEntity.ok("Order accepted orderId: " + orderId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> readOrder(@PathVariable String id) {
        var read = orderRepository.read(id).orElseThrow(() -> new OrderNotFoundException(id));
        Order order = Order.builder().key(id).value(read).build();
        return ResponseEntity.ok().body(order);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<OrderStatusResponse> readStatus(@PathVariable String id) {
        var read = orderRepository.findStatus(id).orElseThrow(() -> new OrderNotFoundException(id));
        OrderStatusResponse response = new OrderStatusResponse(id, read);
        return ResponseEntity.ok().body(response);
    }
}
