package com.hasnain.orderprocessingplatform.controller;

import com.hasnain.orderprocessingplatform.entity.Order;
import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.service.OrderService;
import com.hasnain.orderprocessingplatform.service.UserService;

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
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        User user = userService.getUserById(request.getUserId());
        return orderService.createOrder(user, request.getItems());
    }

    @Getter 
    @Setter 
    public static class CreateOrderRequest {
        private Long userId;
        private Map<Long, Integer> items; // productId to quantity
    }
}
