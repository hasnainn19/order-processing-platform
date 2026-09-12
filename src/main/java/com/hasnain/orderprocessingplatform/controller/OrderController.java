package com.hasnain.orderprocessingplatform.controller;

import com.hasnain.orderprocessingplatform.entity.Order;
import com.hasnain.orderprocessingplatform.service.OrderService;

import lombok.Getter;
import lombok.Setter;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@RestController 
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request.getUserId(), request.getItems());
    }

    @Getter 
    @Setter 
    public static class CreateOrderRequest {
        private Long userId;
        private Map<Long, Integer> items; // productId to quantity
    }
}
