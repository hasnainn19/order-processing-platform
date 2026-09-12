package com.hasnain.orderprocessingplatform.service;

import com.hasnain.orderprocessingplatform.dto.CreateOrderRequest;
import com.hasnain.orderprocessingplatform.dto.OrderResponse;
import com.hasnain.orderprocessingplatform.dto.OrderItemResponse;
import com.hasnain.orderprocessingplatform.entity.Order;
import com.hasnain.orderprocessingplatform.entity.Product;
import com.hasnain.orderprocessingplatform.entity.User;
import com.hasnain.orderprocessingplatform.entity.OrderItem;
import com.hasnain.orderprocessingplatform.repository.OrderRepository;
import com.hasnain.orderprocessingplatform.repository.ProductRepository;
import com.hasnain.orderprocessingplatform.repository.UserRepository;

import com.hasnain.orderprocessingplatform.exception.ResourceNotFoundException;
import com.hasnain.orderprocessingplatform.exception.InsufficientStockException;

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

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional 
    public OrderResponse createOrder(CreateOrderRequest request) {
        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.userId()));

        Order order = new Order();
        order.setUser(user);

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : request.items().entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();

            Product product = productRepository.findById(productId)
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
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        return toResponse(order);
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
