package com.hasnain.orderapi.service;

import com.hasnain.orderapi.dto.CreateOrderRequest;
import com.hasnain.orderapi.dto.OrderResponse;
import com.hasnain.orderapi.dto.PagedResponse;
import com.hasnain.orderapi.dto.OrderItemResponse;
import com.hasnain.orderapi.entity.Order;
import com.hasnain.orderapi.entity.Product;
import com.hasnain.orderapi.entity.User;
import com.hasnain.orderapi.entity.OrderItem;
import com.hasnain.orderapi.repository.OrderRepository;
import com.hasnain.orderapi.repository.ProductRepository;
import com.hasnain.orderapi.repository.UserRepository;

import com.hasnain.orderapi.exception.ResourceNotFoundException;
import com.hasnain.orderapi.messaging.OrderCreatedEvent;
import com.hasnain.orderapi.messaging.RabbitMQConfig;
import com.hasnain.orderapi.exception.InsufficientStockException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository, RabbitTemplate rabbitTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional 
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Order order = new Order();
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : request.items().entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
            
            if (product.getStockQuantity() < quantity) {
                throw new InsufficientStockException("Insufficient stock for product: " + productId);
            }

            product.setStockQuantity(product.getStockQuantity() - quantity);
            productRepository.save(product);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(product.getPrice());
            order.addItem(item);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        }
        order.setTotal(total);
        Order saved = orderRepository.save(order);

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CREATED_ROUTING_KEY, new OrderCreatedEvent(saved.getId()));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findAll(pageable);

        return new PagedResponse<>(
            orderPage.getContent().stream().map(this::toResponse).toList(),
            orderPage.getNumber(),
            orderPage.getSize(),
            orderPage.getTotalElements(),
            orderPage.getTotalPages(),
            orderPage.isLast()
        );
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems()
            .stream()
            .map(this::toItemResponse)
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getUser().getId(),
            order.getUser().getEmail(),
            order.getStatus(),
            order.getTotal(),
            order.getCreatedAt(),
            items
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
            item.getProduct().getId(),
            item.getProduct().getName(),
            item.getQuantity(),
            item.getPrice()
        );
    }
}
