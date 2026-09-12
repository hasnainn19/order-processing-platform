package com.hasnain.orderprocessingplatform.controller;

import com.hasnain.orderprocessingplatform.entity.Order;
import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.service.OrderService;
import com.hasnain.orderprocessingplatform.service.UserService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

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

    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        User user = userService.getUserById(request.getUserId());
        return orderService.createOrder(user, request.getItems());
    }

    public static class CreateOrderRequest {
        private Long userId;
        private Map<Long, Integer> items; // productId to quantity

        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Map<Long, Integer> getItems() {
            return items;
        }

        public void setItems(Map<Long, Integer> items) {
            this.items = items;
        }
    }
}
