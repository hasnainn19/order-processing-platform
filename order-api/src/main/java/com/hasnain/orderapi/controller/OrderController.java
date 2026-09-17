package com.hasnain.orderapi.controller;

import com.hasnain.orderapi.service.OrderService;

import com.hasnain.orderapi.dto.OrderResponse;
import com.hasnain.orderapi.dto.CreateOrderRequest;
import com.hasnain.orderapi.dto.PagedResponse;

import jakarta.validation.Valid;

import com.hasnain.orderapi.security.SecurityUtils;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.Authentication;

@RestController 
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable Long id, Authentication authentication) {
        return orderService.getOrderById(id, authentication.getName(), SecurityUtils.isAdmin(authentication));
    }

    @GetMapping
    public PagedResponse<OrderResponse> getAllOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication) {
        return orderService.getAllOrders(page, size, authentication.getName(), SecurityUtils.isAdmin(authentication));
    }

    @PostMapping
    public OrderResponse createOrder(Authentication authentication, @RequestBody @Valid CreateOrderRequest request) {
        return orderService.createOrder(authentication.getName(), request);
    }
}
